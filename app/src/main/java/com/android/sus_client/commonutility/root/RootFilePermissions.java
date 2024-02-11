package com.android.sus_client.commonutility.root;


import java.io.Serializable;

public final class RootFilePermissions implements Serializable {

    private static final long serialVersionUID = 2682238088276963741L;

    public final boolean ur;
    public final boolean uw;
    public final boolean ux;

    public final boolean gr;
    public final boolean gw;
    public final boolean gx;

    public final boolean or;
    public final boolean ow;
    public final boolean ox;

    public RootFilePermissions(String line) {
        if (line.length() != 10) {
            throw new IllegalArgumentException("Bad permission line");
        }

        this.ur = line.charAt(1) == 'r';
        this.uw = line.charAt(2) == 'w';
        this.ux = line.charAt(3) == 'x';

        this.gr = line.charAt(4) == 'r';
        this.gw = line.charAt(5) == 'w';
        this.gx = line.charAt(6) == 'x';

        this.or = line.charAt(7) == 'r';
        this.ow = line.charAt(8) == 'w';
        this.ox = line.charAt(9) == 'x';
    }

    public RootFilePermissions(boolean ur, boolean uw, boolean ux, boolean gr, boolean gw,
                               boolean gx, boolean or, boolean ow, boolean ox) {
        this.ur = ur;
        this.uw = uw;
        this.ux = ux;

        this.gr = gr;
        this.gw = gw;
        this.gx = gx;

        this.or = or;
        this.ow = ow;
        this.ox = ox;
    }

    public String toOctalPermission() {
        byte user = 0;
        byte group = 0;
        byte other = 0;

        if (ur) {
            user += 4;
        }
        if (uw) {
            user += 2;
        }
        if (ux) {
            user += 1;
        }

        if (gr) {
            group += 4;
        }
        if (gw) {
            group += 2;
        }
        if (gx) {
            group += 1;
        }

        if (or) {
            other += 4;
        }
        if (ow) {
            other += 2;
        }
        if (ox) {
            other += 1;
        }

        return String.valueOf(user) + group + other;
    }
}