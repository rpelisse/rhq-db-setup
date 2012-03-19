RHQ Db Setup:
=============

What is it ?
------------

This small program allow you to setup RHQ's database through a command line, allowing to fully
automated the process of setting up a new instance of RHQ. (Of course, you'll probably need to tweak
other parameter in your RHQ set up - but hopefully thoses tweaks can be achieved using the RHQ CLI).

The tool is packaged as a simple java JAR file and can be run as easily as this:

$ java -cp ./target/jon-setup-1.0-jar-with-dependencies.jar org.redhat.jboss.rqh.autodb.RHQDatabaseInstaller -s http://localhost:7080/installer/start.jsf -u rhqauto -p rhqautopass -d

For full documentation, you can use the provided help:

$ java -cp ./target/jon-setup-1.0-jar-with-dependencies.jar org.redhat.jboss.rqh.autodb.RHQDatabaseInstaller -h

How to build it ?
-----------------

If you want to modify and build this project, you just need to have 'maven' (2.x) and probably a
working internet connection (along with some space on your hard drive). Then, you can just run the
following command:

$ mvn package

This will package the class into a jar, along with all the required dependencies.

Enjoy and don't forget to send pull requests ;)
