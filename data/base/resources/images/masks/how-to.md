The "mask-center-*" images are used for automatically making transitions between base terrain types. Please note that only the alpha channel is used.

These versions should tile perfectly without seams (together and also when continuing the line):
1. "-ne" and "-sw"
2. "-nw" and "-se"

It is possible to use opacity higher than 50%, but in that case remember to reduce the opacity on the other side accordingly.
