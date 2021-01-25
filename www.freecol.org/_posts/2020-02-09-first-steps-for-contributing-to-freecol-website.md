---
  title: First Steps For Contributing To FreeCol Website
  author: Sebastian Zhorel
---
*Sunday, 2021-07-05:
This is a repost from the FreeCol forum to preserve this guide. It includes
a few additional improvements.  
Many thanks to Blake for initially having me write this, trying everything out
causing the improvements and encouraging me now to post the news!*

*Stay tuned for the follow-up: **The Hidden Story Of The Website Update!***


### How To Use Jekyll For The FreeCol Website

Please, follow these steps! If you don't try it can never work.

I just tried every step on my second computer to make sure it works.
I'd like to see if you can follow this before I edit it into the manual:

#### Installation

This is done only once:
- If you are on Windows go to [https://rubyinstaller.org](https://rubyinstaller.org)
then download the recommended version and install it (the Ruby people really
made an effort to make that easy, so please try and just use the standard
options and wait for it to complete -- do **not** try to install Ruby in a path
with spaces; in the command window which opens for the Devkit install you should
**only** type `Enter` once at start and once it completed)
- If you are on Linux use your package manager to install Ruby
- Open a command line window, type `ruby -v`, type `Enter` and check if it
successfully outputs the ruby version
- Type in `gem install jekyll bundler wdm` and `Enter` to install Jekyll;
it should complete without an error (if not please ask)
- You could try typing in `jekyll -v` to see if it got installed correctly

#### Use

This is done every time you want to see the effect of your edits:
- Open a command line window for the `www.freecol.org` directory where
you put your local clone of the freecol git repository (if you have git
installed on Windows just find the directory in Windows Explorer and
right click and choose `Git bash here`; otherwise you would need to navigate
through `cd ..` and `cd _directoryname_` and show contents using
`dir` (Windows) or `ls` (Linux) to get to the right directory)
- If you are inside the `www.freecol.org` directory you just type
`jekyll serve` to compile the website and start a local server on this
computer
- Open your browser and type `127.0.0.1:4000` in the address bar (thats the
address of this local computer of yours with the port of the local server
started by above command)
- Edit the website files as you like (Jekyll automatically recompiles it
as long as you did not stop it or close the command window), reload (`F5`) in
browser to see your changes
- You can stop the local server using `Ctrl+C` to not have it use
system recources of your computer when you take a break or need a
long time editing and restart it using same `jekyll serve` command
- `jekyll build` only compiles the website without starting the local server,
in case you want to inspect the compilation result inside the `_site` directory
or upload it to the webserver
- In case you accidently started `jekyll serve` or `jekyll build` in the
wrong directory use `jekyll clean` to let it clean up the temporary files
it created

Please, tell if everything worked!
