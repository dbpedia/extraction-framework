# Git Workflow for Issue #804 Fix

## Step-by-Step Guide (From README.md)

### ✅ Step 1: Switch to dev branch
```powershell
git checkout dev
```

### ✅ Step 2: Create new feature branch
```powershell
git checkout dev -b fix/issue-804-macedonian-template-namespace
```

**Branch naming:** Use something meaningful:
- ✅ `fix/issue-804-macedonian-template-namespace`
- ✅ `fix/issue-804`  
- ❌ `fix1` (too vague)

### ✅ Step 3: Verify you're on the new branch
```powershell
git branch
# Should show: * fix/issue-804-macedonian-template-namespace
```

### ✅ Step 4: View your changes
```powershell
git status
# Should show modified: server/src/main/scala/org/dbpedia/extraction/server/stats/MappingStatsHolder.scala
```

### ✅ Step 5: Stage and commit your changes
```powershell
# Stage the modified file
git add server/src/main/scala/org/dbpedia/extraction/server/stats/MappingStatsHolder.scala

# Commit with proper message (First line <= 70 characters)
git commit -m "Fix #804: Handle multiple template namespace prefixes for Macedonian

- Macedonian Wikipedia uses both 'Предлошка:' and 'Шаблон:' for templates
- Previous code assumed single prefix, causing StringIndexOutOfBoundsException
- Build set of ALL valid prefixes from Namespaces.names(language)
- Only process templates that match a valid namespace prefix
- Fixes crashes in both template processing and redirect filtering
- Backwards compatible with all other languages"
```

### ✅ Step 6: Push branch to GitHub
```powershell
# First, make sure you have forked the repo on GitHub
# Then:
git push origin fix/issue-804-macedonian-template-namespace
```

### ✅ Step 7: Create Pull Request on GitHub
- Go to: https://github.com/YOUR_USERNAME/extraction-framework
- Click "Pull Request" button
- Select:
  - From: `fix/issue-804-macedonian-template-namespace` (your branch)
  - To: `dbpedia/extraction-framework` → `dev` (their dev branch)
- Add description:
  ```
  ## Description
  Fixes #804 - Server crashes when processing Macedonian templates with 'Шаблон:' prefix
  
  ## Problem
  Macedonian Wikipedia has two valid template namespace prefixes ('Предлошка:' and 'Шаблон:'), 
  but the code only checked for one, causing StringIndexOutOfBoundsException.
  
  ## Solution
  - Query all namespace names for Template namespace (code 10) from Namespaces.names(language)
  - Build a Set of valid prefixes for the current language
  - Match templates against any valid prefix instead of just the canonical one
  - Updated both template processing and redirect filtering logic
  
  ## Testing
  - Code compiles without errors
  - Backwards compatible with all other languages
  - No breaking changes to existing functionality
  ```

---

## What Does Each Command Mean?

| Command | Meaning |
|---------|---------|
| `git checkout dev` | Switch to the dev branch |
| `git checkout dev -b fix/issue-804` | Create NEW branch from dev and switch to it |
| `git branch` | List all branches (shows which one is active with *) |
| `git status` | Show what files changed |
| `git add <file>` | Stage file for commit |
| `git commit -m "message"` | Save changes with a message |
| `git push origin <branch>` | Send your branch to GitHub |
| `2>&1` | (Shell thing - ignore, not important) |

---

## Current Status

✅ **Our Fix is Ready:**
- MappingStatsHolder.scala has been modified correctly
- Code compiles without errors
- No tests required (optional for simple fixes)

✅ **Next Actions:**
1. Create feature branch: `git checkout dev -b fix/issue-804-macedonian-template-namespace`
2. Commit the change
3. Push to GitHub
4. Create Pull Request to `dbpedia/extraction-framework:dev`

---

## Important Notes

### ⚠️ DO NOT use `git reset --hard`
- It deletes ALL your changes (including our fix)
- Only use if you want to completely undo everything

### ✅ DO use clean commits
- One logical change per commit
- Good commit messages help reviewers understand

### ✅ DO reference the issue in PR
- Use "Fixes #804" in description
- GitHub will auto-link and close the issue

---

## Ready to Proceed?

Once you're ready to commit, just run:

```powershell
# 1. Switch to dev
git checkout dev

# 2. Create feature branch  
git checkout dev -b fix/issue-804-macedonian-template-namespace

# 3. Add and commit
git add server/src/main/scala/org/dbpedia/extraction/server/stats/MappingStatsHolder.scala
git commit -m "Fix #804: Handle multiple template namespace prefixes for Macedonian

- Macedonian Wikipedia uses both 'Предлошка:' and 'Шаблон:' for templates
- Previous code assumed single prefix, causing StringIndexOutOfBoundsException
- Build set of ALL valid prefixes from Namespaces.names(language)
- Only process templates that match a valid namespace prefix
- Fixes crashes in both template processing and redirect filtering
- Backwards compatible with all other languages"

# 4. Push to GitHub
git push origin fix/issue-804-macedonian-template-namespace
```

Then create PR on GitHub! 🎉
