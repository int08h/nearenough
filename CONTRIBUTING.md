Contributions to Nearenough are welcomed!

## Process

1. Fork the repo and create a feature branch for your work
2. Commit your work on the branch
3. Ensure all tests pass
4. Open a Pull Request
5. Discuss and iterate

Notes
* Please keep pull requests limited to a single issue.
* Whitespace-only commits should be separate from other code changes. You can commit your new feature
  or fix first and then follow with a separate whitespace/style commit.

## Testing

* New features/functionality require new tests. 
* Please run the test suite and ensure it passes.
* Bugfixes should be accompanied by a test case covering the bug.

## Style/Coding Standard

* Java 8 
* Java code must comply with the
  [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). This is strictly
  enforced.
* All files use unix (LF) line endings

## External Dependencies

Nearenough should be as unencumbered as possible. The `protocol` and `client` packages in particular
strive to be bare-bones and may depend only on the Java 8 standard library, EdDSA-Java, and Netty.

## Licensing of Contributions

Section 5 of the Apache 2.0 license describes how contributions are handled:

   > Submission of Contributions. Unless You explicitly state otherwise,
   > any Contribution intentionally submitted for inclusion in the Work
   > by You to the Licensor shall be under the terms and conditions of
   > this License, without any additional terms or conditions.
   > Notwithstanding the above, nothing herein shall supersede or modify
   > the terms of any separate license agreement you may have executed
   > with Licensor regarding such Contributions.
      
### Contributor License Agreement

One-liners, simple fixes, and other minor changes do **not** require a CLA. They are handled as per 
Section 5 of the Apache 2.0 license. Prior to being merged into `master` you will be asked:

  1. To confirm you are the original author of the work.
  2. To state that you license the work to the Nearenough project under the Apache 2.0 license and 
     that you have the legal authority to do so.

Substantial contributions **do** require a completed Contributor License Agreement (CLA) prior to 
them being accepted. The definition of "substantial" is fuzzy, but we'll know it when we see it. 
Please contact stuart{at}int08h.com to get the CLA submitted.



