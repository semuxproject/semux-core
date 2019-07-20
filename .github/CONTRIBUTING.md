# Contributing to Semux

Anyone is welcome to contribute towards development in the form of peer review, testing and patches. This document explains the practical process and guidelines for contributing.

## Code conventions

This project follows the default Eclipse Java code style, with a larger line limit. Here are some highlights:
```
indent: no tab, 4 spaces instead
line limit: 120 chars
```

To format your code, run:
```
mvn formatter:format license:format
```

To check code style and potential security bugs, run:
```
mvn formatter:validate license:check findbugs:check
```

For IDE setup, visit [here](../docs/IDE-Setup.md).

## Contributor workflow

To contribute a patch, the workflow is as follows:

  1. Fork repository
  2. Create topic branch
  3. Commit patches

In general [commits should be atomic](https://en.wikipedia.org/wiki/Atomic_commit#Atomic_commit_convention) and diffs should be easy to read. For this reason do not mix any formatting fixes or code moves with actual code changes.

Commit messages should be verbose by default consisting of a short subject line (**imperative present tense, max 50 characters, prefixed by component**), a blank line and detailed explanatory text as separate paragraph(s), unless the title alone is self-explanatory in which case a single title line is sufficient. Commit messages should be helpful to people reading your code in the future, so explain the reasoning for your decisions. Further explanation [here](https://github.com/agis/git-style-guide). Example:

```
Component: short summary of changes

More detailed explanatory text, if necessary. In some contexts, the first
line is treated as the subject of an email and the rest of the text as the body.
The blank line separating the summary from the body is critical.

Further paragraphs come after blank lines.

Resolves: #56, #78
See also: #12, #34
```

The title of the pull request should be prefixed by the component or area that the pull request affects. Valid areas as:

  - **API** for changes to the Semux RESTful API
  - **CLI** for changes to the command line interface
  - **Config** for changes to the configurations and specifications
  - **Consensus** for changes to the Semux BFT consensus
  - **Core** for changes to the core data structures, procedures and algorithms
  - **Crypto** for changes to the crypto library
  - **DB** for changes to the database implementation
  - **GUI** for changes to the GUI
  - **Net** OR **P2P** for changes to the P2P protocol implementation
  - **Util** for changes to the utilities and libraries
  - **VM** for changes to the virtual machine
  - **Docs** for changes to the documentations
  - **Tests** for changes to the unit tests
  - **Tools** for changes to the scripts and tools
  - **Trivial** should **only** be used for PRs that do not change generated executable code:
    - comments
    - whitespace
    - variable names
    - logging and messages

Examples:
```
Consensus: adjust the BFT timeout parameters
P2P: increase the max allowed connections
Trivial: fix typo in Semux.java
```

If a pull request is not to be considered for merging (yet), please prefix the title with [WIP] or use [Tasks Lists](https://help.github.com/articles/basic-writing-and-formatting-syntax/#task-lists) in the body of the pull request to indicate tasks are pending.

The body of the pull request should contain enough description about what the patch does together with any justification/reasoning. You should include references to any discussions (for example other tickets or mailing list discussions).

At this stage one should expect comments and review from other contributors. You can add more commits to your pull request by committing them locally and pushing to your fork until you have satisfied all feedback.


## Squashing commits

If your pull request is accepted for merging, you may be asked by a maintainer to squash and or [rebase](https://git-scm.com/docs/git-rebase) your commits before it will be merged. The basic squashing workflow is shown below.

```
git checkout your_branch_name
git rebase -i HEAD~n
# n is normally the number of commits in the pull
# set commits from 'pick' to 'squash', save and quit
# on the next screen, edit/refine commit messages
# save and quit
git push -f # (force push to GitHub)
```

## Pull request philosophy

Patchsets should always be focused. For example, a pull request could add a feature, fix a bug, or refactor code; but not a mixture. Please also avoid super pull requests which attempt to do too much, are overly large, or overly complex as this makes review difficult.


## Copyright

By contributing to this repository, you agree to license your work under the MIT license. Any work contributed where you are not the original author must contain its license header with the original author(s) and source.

```
/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
```
