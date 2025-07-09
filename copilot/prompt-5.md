Past failed efforts show that it's difficult to test the CLI main program as it is.
Sometimes it exits, or does shutdown-agents, which kills the REPL running the tests.
System/exit can't be mocked.
This time our approach is to extract the guts of the main function into a handler,
which accepts the CLI arguments but also functions like exit that would be disruptive
to call and which can't be mocked.
This way, our test can exercise the handler by passing in mocks.
I'm willing to leave the main function out of this test for now.
The risky code lies in argument parsing, dispatching logic, acting on results, which is what the handler does.
