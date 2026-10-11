<#
.SYNOPSIS
Fetch and compare the latest stable Element X release; optionally prepare a merge.
.EXAMPLE
powershell -ExecutionPolicy Bypass -File tools/upstream/update-element-x.ps1
.EXAMPLE
powershell -ExecutionPolicy Bypass -File tools/upstream/update-element-x.ps1 -Merge
#>
[CmdletBinding()]
param(
    [string]$Tag,
    [switch]$IncludePrerelease,
    [switch]$Merge,
    [switch]$NoFetch,
    [string]$Repository
)
$ErrorActionPreference = 'Stop'
if (!$Repository) { $Repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path }

function Invoke-Git {
    param([string[]]$Arguments)
    $result = & git -C $Repository @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Git failed: git $($Arguments -join ' ')" }
    return $result
}

try {
    if ($NoFetch -and !$Tag) { throw '-NoFetch requires an explicit, previously fetched -Tag.' }
    if ($Merge -and (Invoke-Git @('status', '--porcelain'))) {
        throw 'Commit or stash your local changes (including untracked files) before merging. Nothing was changed.'
    }
    $upstreamUrl = 'https://github.com/element-hq/element-x-android.git'
    $remotes = @(Invoke-Git @('remote'))
    if ($remotes -contains 'upstream') {
        $configuredUrl = Invoke-Git @('remote', 'get-url', 'upstream')
        if ($configuredUrl -notin @($upstreamUrl, 'git@github.com:element-hq/element-x-android.git', ($upstreamUrl -replace '\.git$', ''))) {
            throw "The upstream remote points to a different repository: $configuredUrl"
        }
    } elseif (!$NoFetch) {
        Invoke-Git @('remote', 'add', 'upstream', $upstreamUrl)
    }
    if (!$Tag) {
        $headers = @{ 'User-Agent' = 'Matrix-Classic-upstream-update'; 'Accept' = 'application/vnd.github+json' }
        $api = 'https://api.github.com/repos/element-hq/element-x-android/releases'
        if ($IncludePrerelease) {
            $releases = Invoke-RestMethod -Uri ($api + '?per_page=100') -Headers $headers
            $release = $releases | Where-Object { !$_.draft } | Select-Object -First 1
        } else {
            $release = Invoke-RestMethod -Uri ($api + '/latest') -Headers $headers
        }
        if (!$release) { throw 'No eligible upstream release was found.' }
        $Tag = $release.tag_name
        Write-Host "Release: $($release.html_url) (prerelease: $($release.prerelease))"
    }
    if ($Tag -notmatch '^v\d{2}\.\d{2}\.\d+$') { throw "Unsupported release tag: $Tag" }
    if (!$NoFetch) { Invoke-Git @('fetch', 'upstream', "refs/tags/${Tag}:refs/tags/${Tag}") }
    $target = Invoke-Git @('rev-parse', '--verify', "refs/tags/${Tag}^{commit}")
    & git -C $Repository merge-base --is-ancestor $target HEAD
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Already includes Element X $Tag."
        exit 0
    }
    if ($LASTEXITCODE -ne 1) { throw 'Could not compare the release with HEAD.' }
    $base = Invoke-Git @('merge-base', 'HEAD', $target)
    Write-Host "Upstream target: $Tag ($target)"
    Invoke-Git @('diff', '--shortstat', $base, $target)
    if (!$Merge) {
        Write-Host 'Fetched only. Run again with -Merge after committing or stashing customisations.'
        exit 0
    }
    # Screenshot LFS objects are not required to integrate or build the source.
    $previousSmudge = $env:GIT_LFS_SKIP_SMUDGE
    try {
        $env:GIT_LFS_SKIP_SMUDGE = '1'
        Invoke-Git @('merge', '--no-ff', '--no-commit', $target)
    } finally {
        $env:GIT_LFS_SKIP_SMUDGE = $previousSmudge
    }
    Write-Host 'Merge prepared. Resolve conflicts, run the home/media/navigation tests and build the APK.'
    Write-Host "Then commit with a title and description, e.g. git commit -m 'Update Element X core to $Tag' -m 'Merge the upstream release and preserve Matrix Classic customisations.'"
    Write-Host 'To cancel this pending merge: git merge --abort'
} catch {
    $mergeHead = & git -C $Repository rev-parse --git-path MERGE_HEAD 2>$null
    if ($LASTEXITCODE -eq 0) {
        $mergeHeadPath = if ([IO.Path]::IsPathRooted($mergeHead)) { $mergeHead } else { Join-Path $Repository $mergeHead }
        if (Test-Path -LiteralPath $mergeHeadPath) {
            Write-Host 'A merge is pending. Resolve conflicts and test before committing, or cancel with git merge --abort.'
        }
    }
    Write-Error $_ -ErrorAction Continue
    exit 1
}
