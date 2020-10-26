#!/usr/bin/awk -f
BEGIN { n = 0; m = 0.0; } {
    if (NF == 1) {
        n++;
        a[$1]++;
        m += $1;
    } else if (NF == 2) {
        n += $1;
        a[$2] += $1;
        m += $1 * $2;
    }
} END {
    v = 0.0;
    if (n != 0) {
        m /= n;
        for (x in a) {
            v += (x - m) * (x - m) * a[x];
        }
        v = sqrt(v / n);
    }
    printf "n=%d mean=%f sd=%f\n", n, m, v;
}
