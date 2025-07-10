Let's Implement the evaluate Command.
Create a new pcrit.command.evaluate namespace.

The Original proposal said:
  This command will take a generation number and a contest name as arguments.

It should evaluate the current (most recent) generation if no generation number is given.
If no contest name is given, it should default to "contest".
If the contest name given already exists in that generation, it should fail.

evaluate will prepare a failter-spec directory by creating symlinks to the specified generation's population and the user-provided input data.
It will then execute the external failter toolchain.
Finally, it will capture the resulting report.csv and store it in the appropriate contest directory (.../contests/<contest-name>/).

Is there any reason evaluate wouldn't work on generation 0, as produced by bootstrap?
In other words, would bootstrap immediately followed by evaluate do something sensible?

Produce a directory listing of the most recent generation directory before and
after the evaluate command is run using the default arguments.
Use examples for the prompt population, but keep it small.
