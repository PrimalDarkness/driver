Primal Darkness Driver - a precompiled, FluffOS container
======================

The Primal Darkness Driver is the container image used to run the MUD
at https://primaldarkness.com.

Usage
-----

`docker run -v /path/to/lib:/mud/library -v $PWD:/mud/config primaldarkness/driver config.sample.mud`

Notes
-----

This is not the full Primal Darkness game, only the compiled driver that it runs on. You must
provide your own LP based mudlib in order to utilize this container. Check 
out [lpmuds.net](http://lpmuds.net/downloads.html) for downloadable MUDLibs. Compatibility not
likely.
