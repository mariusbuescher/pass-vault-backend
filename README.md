# PassVault

> PassVault is a vault for your passwords.

The idea behind PassVault is that the passwords are e2e encrypted using pairs of keys.
The encryption/decryption process is done in the frontend, so only you can read your
passwords as long as you keep your private key private. ;-)

This piece should be used as the central storage backend on a server. Any Server should
be able to do it, as long as it supports running an application compiled to Java Bytecode.

## Prerequisites

As mentioned above this software is compiled to Java Bytecode. It is implemented in Kotlin
and uses PostgreSQL as a database backend. You only need a running database and gradle
at least in version 4.5 installed on your machine.

## Setup

First make sure you run all the database migrations in your database. Then copy the `.env.dist`
to `.env` and adjust the variables to your need. Then type `gradle run` in your terminal
and the application should be up and running.
