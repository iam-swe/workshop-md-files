---
name: "update_documentation"
description: "Update DOCUMENTATION.md with latest commits by invoking generate_documentation prompt."
applyTo: "DOCUMENTATION.md"
---

# Instructions for Copilot

1. Run the Python helper script `scripts/update_docs.py` to gather context:  
   - Last documented commit from `DOCUMENTATION.md`  
   - New commits on `main` branch since then  
   - Changed files and their contents  

2. Take this context and modify the documentation based on the files changed or added.  

3. Update the contents of `DOCUMENTATION.md`.  
   - Keep all existing sections from `generate_documentation` prompt.  
   - Update commit placeholder `**Last Updated Commit:** \`<HASH>\`` to the latest commit hash.  
   - Ensure changes are integrated cleanly without duplication.  



