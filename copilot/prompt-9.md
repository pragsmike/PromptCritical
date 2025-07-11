Write an integration test that exercises the CLI, simulating what a user would be able to do.
It can run the CLI script, and whatever else a user could do from a shell.
It cannot execute Clojure code directly.
Its only observation points are the stdout and stderr of the commands
and the files in the filesystem.

The test should
   * creates an experiment directory
   * does bootstrap, confirming that the expected files were created
   * does evaluate, confirming that the expected files were created
   * does vary, confirming that the expected files were created
   * does select, confirming that the expected files were created

Then prove that the files in the experiment directory don't depend on its
absolute path (symlinks are all relative):
   * Move that same experiment directory to a new name (in the same parent
     directory), preserving all the files under it.
   * do evaluate, vary, select building on the files created earlier
   * check that the expected files exist

What are the pros and cons of writing this test as a shell script?
