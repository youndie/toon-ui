// B-59: make the stage tiles seamless, from the design's own (research D13).
//
//   java scripts/seamless-tiles.java <design tile.png> <bundled tile.png>
//
// The design's 512 px tiles do not wrap: the gouache tiles' last row stands twice the inner
// difference from the first, a line across the stage every tile, and paper night's last column is a
// step too. This folds the last OVERLAP columns onto the first, then the last rows onto the first,
// cutting rather than blending (image quilting): across the overlap no pixel is mixed, a path one
// step a line at most divides the folded end from the tile's own start where the two agree best,
// and its ends meet, so the other axis still wraps. The tile comes out OVERLAP smaller each way,
// keeps its five to nine posterised colours and its grain, and shows each part of the design's tile
// once. Two alternatives were tried and refused: a blend with the tile rolled by half shows the
// tile's middle twice, a repeat at half the period that ToonStageTest catches; a cross-fade of the
// overlap, even variance-kept and put back on the palette, leaves a line where the weights are even.

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class SeamlessTiles {
    /** How much of each axis is folded onto the other end: 512 px tiles become 448. */
    static final int OVERLAP = 64;

    /** The cut keeps this far from the overlap's edges, so every line takes some of both sides. */
    static final int MARGIN = 4;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        int w = img.getWidth(), h = img.getHeight();
        int[][] c = new int[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) c[y][x] = img.getRGB(x, y) & 0xFFFFFF;
        for (int pass = 0; pass < 2; pass++) {
            boolean across = pass == 0;
            int nw = across ? w - OVERLAP : w, nh = across ? h : h - OVERLAP;
            int lines = across ? nh : nw;
            double[][] err = new double[lines][OVERLAP];
            for (int l = 0; l < lines; l++)
                for (int i = 0; i < OVERLAP; i++) {
                    int x = across ? i : l, y = across ? l : i;
                    err[l][i] = distance(c[y][x], c[across ? y : y + nh][across ? x + nw : x]);
                }
            int[] cut = closedCut(err);
            int[][] next = new int[nh][nw];
            for (int y = 0; y < nh; y++)
                for (int x = 0; x < nw; x++) {
                    int along = across ? x : y, l = across ? y : x;
                    next[y][x] = along < cut[l] ? c[across ? y : y + nh][across ? x + nw : x] : c[y][x];
                }
            c = next;
            w = nw;
            h = nh;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setRGB(x, y, c[y][x]);
        ImageIO.write(out, "png", new File(args[1]));
    }

    static double distance(int p, int q) {
        double s = 0;
        for (int k = 0; k < 3; k++) s += Math.pow(((p >> (8 * k)) & 255) - ((q >> (8 * k)) & 255), 2);
        return s;
    }

    /** The cheapest path through [err] (lines × positions), at most one position a line, ending next to its start. */
    static int[] closedCut(double[][] err) {
        int n = err.length, o = err[0].length;
        double best = Double.MAX_VALUE;
        int[] path = null;
        for (int s = MARGIN; s < o - MARGIN; s++) {
            double[][] cost = new double[n][o];
            int[][] from = new int[n][o];
            for (double[] r : cost) Arrays.fill(r, Double.MAX_VALUE);
            cost[0][s] = err[0][s];
            for (int l = 1; l < n; l++)
                for (int i = MARGIN; i < o - MARGIN; i++)
                    for (int j = Math.max(MARGIN, i - 1); j <= Math.min(o - MARGIN - 1, i + 1); j++) {
                        if (cost[l - 1][j] == Double.MAX_VALUE) continue;
                        double v = cost[l - 1][j] + err[l][i];
                        if (v < cost[l][i]) {
                            cost[l][i] = v;
                            from[l][i] = j;
                        }
                    }
            for (int e = Math.max(MARGIN, s - 1); e <= Math.min(o - MARGIN - 1, s + 1); e++) {
                if (cost[n - 1][e] >= best) continue;
                best = cost[n - 1][e];
                path = new int[n];
                path[n - 1] = e;
                for (int l = n - 1; l > 0; l--) path[l - 1] = from[l][path[l]];
            }
        }
        return path;
    }
}
