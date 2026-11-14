**Open Source Submodule Fork Information**

This repository is an open-source fork of [sora-editor](https://github.com/Rosemoe/sora-editor)
maintained as a submodule for my personal project.
- **Target Platform:** Android API 30+ (Android 11 and above) for `editor`, `editor-lsp`, and
    `language-textmate`.
- **Purpose:** Dedicated to experimental features, custom optimizations, and breaking API changes
    tailored for my personal project.
- WILL NEVER MERGE TO UPSTREAM.

Implements Many features and Bugs Fixes from issues and PRs in the [Upstream](https://github.com/Rosemoe/sora-editor) 

This Fork: 
1. Kotlin First.
2. Android API 30 or Up.
3. Uses New RenderNode and Newer Android APIs
4. Compose First Implementation of Popups however the CodeEditor is not Converted to Compose first because Compose lacks some IME handling. so it has wrapper for compose. 
5. Has Inline Completions. (aka GhostText)
6. Some Bugs and Performance Fixes.
7. Hand Written Code (AI Written Docs and KDoc)
8. Most of the Architecture is intact only Removed Legacy Code, Checks, and Moved to Kotlin Pure/Native mutableList, state flows.

Check the Original [Sora Editor](https://github.com/Rosemoe/sora-editor) 

## License

```
sora-editor - the awesome code editor for Android
https://github.com/Rosemoe/sora-editor
Copyright (C) 2020-2026  Rosemoe

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
USA

Please contact Rosemoe by email 2073412493@qq.com if you need
additional information or have any questions
```

## Acknowledgements

Thanks to Rosemoe for such a great Code Editor. 
