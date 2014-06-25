Miranda
=======

Python client library for Fowles.

Under the hood, it uses ZeroMQ to:
  + PUSH input into Fowles
  + PULL output from Fowles

You need not do both of these in the same process.  In fact, for
simplicity of coding it's likely better to create two separate
clients, one for PUSHing and another for PULLing.

The use of ZeroMQ is abstracted from you, except that you have to tell
your Python program "where" Fowles is.

    connect_to_Fowles()