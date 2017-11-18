# Contributing to Semux

Anyone is welcome to contribute towards development in the form of peer review, testing and patches. This document explains the practical process and guidelines for contributing.

## Code conventions

This project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with additional requirements.
```
indent: no tab, 4 spaces instead
line limit: 120 chars
```

To format your code, please run the following command:
```
mvn formatter:format
```

An [Eclipse Java formatter](https://raw.githubusercontent.com/semuxproject/semux/master/misc/eclipse/formatter.xml) is also provided.

## Contributor workflow

To contribute a patch, the workflow is as follows:

  1. Fork repository
  2. Create topic branch
  3. Commit patches

In general [commits should be atomic](https://en.wikipedia.org/wiki/Atomic_commit#Atomic_commit_convention) and diffs should be easy to read. For this reason do not mix any formatting fixes or code moves with actual code changes.

Commit messages should be verbose by default consisting of a short subject line (**imperative present tense, capitalized, max 50 characters**), a blank line and detailed explanatory text as separate paragraph(s), unless the title alone is self-explanatory in which case a single title line is sufficient. Commit messages should be helpful to people reading your code in the future, so explain the reasoning for your decisions. Further explanation [here](https://github.com/agis/git-style-guides).

If a particular commit references another issue, please add the reference. For example: `refs #1234` or `fixes #4321`. Using the `fixes` or `closes` keywords will cause the corresponding issue to be closed when the pull request is merged.

Please refer to the [Git manual](https://git-scm.com/doc) for more information about Git.

  - Push changes to your fork
  - Create pull request

The title of the pull request should be prefixed by the component or area that
the pull request affects. Valid areas as:

  - **API** for changes to the RESTful API code
  - **CLI** for changes to the wallet CLI code
  - **Consensus** for changes to the consensus code
  - **Core** for changes to the core data structures and algorithms
  - **Crypto** for changes to the crypto code
  - **DB** for changes to the database code
  - **GUI** for changes to the wallet GUI code
  - **Net** for changes to the peer-to-peer network code
  - **Util** for changes to the utils and libraries
  - **VM** for changes to the database code
  - **Docs** for changes to the docs
  - **Tests** for changes to the unit test and QA tests
  - **Tools** for changes to the scripts and tools
  - **Trivial** should **only** be used for PRs that do not change generated executable code:
    - comments
    - whitespace
    - variable names
    - logging and messages

Examples:
```
Consensus: Adjust the BFT timeout parameters
P2P: Increase the max allowed connections
Trivial: Fix typo in Semux.java
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
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
```
