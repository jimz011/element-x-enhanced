"""Exercise the update helper in disposable Git repositories without network access."""
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("update-element-x.ps1").resolve()


class UpdateHelperTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="matrix-upstream-test-")
        self.repo = Path(self.temp.name)
        self.git("init", "-b", "fork")
        self.git("config", "user.name", "Update helper test")
        self.git("config", "user.email", "test@example.invalid")
        self.git("config", "commit.gpgsign", "false")
        (self.repo / "core.txt").write_text("old core\n")
        self.git("add", ".")
        self.git("commit", "-m", "Base")
        self.git("checkout", "-b", "release")
        (self.repo / "core.txt").write_text("updated core\n")
        self.git("commit", "-am", "Release")
        self.git("tag", "v26.09.2")
        self.git("checkout", "fork")
        (self.repo / "classic.txt").write_text("custom UX\n")
        self.git("add", ".")
        self.git("commit", "-m", "Customisation")

    def tearDown(self):
        self.temp.cleanup()

    def git(self, *args):
        result = subprocess.run(["git", "-C", str(self.repo), *args], capture_output=True, text=True, check=True)
        return result.stdout.strip()

    def run_helper(self, *args, tag="v26.09.2"):
        return subprocess.run(
            ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(SCRIPT),
             "-Repository", str(self.repo), "-Tag", tag, "-NoFetch", *args],
            capture_output=True, text=True,
        )

    def test_report_preserves_dirty_files_and_history(self):
        before = self.git("rev-parse", "HEAD")
        (self.repo / "classic.txt").write_text("uncommitted customisation\n")
        result = self.run_helper()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Fetched only", result.stdout)
        self.assertEqual(self.git("rev-parse", "HEAD"), before)
        self.assertEqual((self.repo / "classic.txt").read_text(), "uncommitted customisation\n")

    def test_merge_rejects_untracked_changes(self):
        (self.repo / "draft.txt").write_text("draft\n")
        result = self.run_helper("-Merge")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Commit or stash", result.stderr)
        self.assertFalse((self.repo / ".git/MERGE_HEAD").exists())

    def test_clean_merge_updates_core_and_preserves_customisation(self):
        result = self.run_helper("-Merge")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((self.repo / ".git/MERGE_HEAD").exists())
        self.assertEqual((self.repo / "core.txt").read_text(), "updated core\n")
        self.assertEqual((self.repo / "classic.txt").read_text(), "custom UX\n")
        self.git("commit", "-m", "Merge release")
        result = self.run_helper()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Already includes", result.stdout)

    def test_foreign_upstream_remote_is_rejected(self):
        self.git("remote", "add", "upstream", "https://example.invalid/foreign.git")
        result = self.run_helper()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("different repository", result.stderr)

    def test_conflicts_keep_a_pending_merge_that_can_be_aborted(self):
        (self.repo / "core.txt").write_text("custom core change\n")
        self.git("commit", "-am", "Conflicting custom core")
        result = self.run_helper("-Merge")
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue((self.repo / ".git/MERGE_HEAD").exists())
        self.git("merge", "--abort")
        self.assertEqual((self.repo / "core.txt").read_text(), "custom core change\n")

    def test_invalid_tag_is_rejected(self):
        result = self.run_helper(tag="invalid")
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
