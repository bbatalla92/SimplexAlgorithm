package bgb.com.simplexalgorithm;

import android.util.Log;

        public class Simplex {
        private static String TAG = "Simplex";

        private static final double EPSILON = 1.0E-10;
        private double[][] a;   // tableaux
        private int M;          // number of constraints
        private int N;          // number of original variables

        private int[] basis;    // basis[i] = basic variable corresponding to row i
        // only needed to print out solution, not book

        // sets up the simplex tableaux
        public Simplex(double[][] A, double[] b, double[] c){
            M = b.length;
            N = c.length;
            a = new double[M + 1][N + M + 1];
            for (int i = 0; i < M; i++)
                for (int j = 0; j < N; j++)
                    a[i][j] = A[i][j];
            for (int i = 0; i < M; i++) a[i][N + i] = 1.0;
            for (int j = 0; j < N; j++) a[M][j] = c[j];
            for (int i = 0; i < M; i++) a[i][M + N] = b[i];

            basis = new int[M];
            for (int i = 0; i < M; i++) basis[i] = N + i;

            solve();

            // check optimality conditions
            assert check(A, b, c);
        }

        // run simplex algorithm starting from initial BFS

    private void solve() {
        while (true) {

            // find entering column q
            int q = bland();
            if (q == -1) break;  // optimal

            // find leaving row p
            int p = minRatioRule(q);
            if (p == -1) throw new ArithmeticException("Linear program is unbounded");

            // pivot
            pivot(p, q);

            // update basis
            basis[p] = q;
        }
    }

    // lowest index of a non-basic column with a positive cost
    private int bland() {
        for (int j = 0; j < M + N; j++)
            if (a[M][j] > 0) return j;
        return -1;  // optimal
    }

    // index of a non-basic column with most positive cost
    private int dantzig() {
        int q = 0;
        for (int j = 1; j < M + N; j++)
            if (a[M][j] > a[M][q]) q = j;

        if (a[M][q] <= 0) return -1;  // optimal
        else return q;
    }

    // find row p using min ratio rule (-1 if no such row)
    private int minRatioRule(int q) {
        int p = -1;
        for (int i = 0; i < M; i++) {
            if (a[i][q] <= 0) continue;
            else if (p == -1) p = i;
            else if ((a[i][M + N] / a[i][q]) < (a[p][M + N] / a[p][q])) p = i;
        }
        return p;
    }

    // pivot on entry (p, q) using Gauss-Jordan elimination
    private void pivot(int p, int q) {

        // everything but row p and column q
        for (int i = 0; i <= M; i++)
            for (int j = 0; j <= M + N; j++)
                if (i != p && j != q) a[i][j] -= a[p][j] * a[i][q] / a[p][q];

        // zero out column q
        for (int i = 0; i <= M; i++)
            if (i != p) a[i][q] = 0.0;

        // scale row p
        for (int j = 0; j <= M + N; j++)
            if (j != q) a[p][j] /= a[p][q];
        a[p][q] = 1.0;
    }

    // return optimal objective value
    public double value() {
        return -a[M][M + N];
    }

    // return primal solution vector
    public double[] primal() {
        double[] x = new double[N];
        for (int i = 0; i < M; i++)
            if (basis[i] < N) x[basis[i]] = a[i][M + N];
        return x;
    }

    // return dual solution vector
    public double[] dual() {
        double[] y = new double[M];
        for (int i = 0; i < M; i++)
            y[i] = -a[M][N + i];
        return y;
    }


    // is the solution primal feasible?
    private boolean isPrimalFeasible(double[][] A, double[] b) {
        double[] x = primal();

        // check that x >= 0
        for (int j = 0; j < x.length; j++) {
            if (x[j] < 0.0) {
                Log.e(TAG,"x[" + j + "] = " + x[j] + " is negative");
                return false;
            }
        }

        // check that Ax <= b
        for (int i = 0; i < M; i++) {
            double sum = 0.0;
            for (int j = 0; j < N; j++) {
                sum += A[i][j] * x[j];
            }
            if (sum > b[i] + EPSILON) {
                Log.e(TAG, "not primal feasible");
                Log.e(TAG, "b[" + i + "] = " + b[i] + ", sum = " + sum);
                return false;
            }
        }
        return true;
    }

    // is the solution dual feasible?
    private boolean isDualFeasible(double[][] A, double[] c) {
        double[] y = dual();

        // check that y >= 0
        for (int i = 0; i < y.length; i++) {
            if (y[i] < 0.0) {
                Log.e(TAG, "y[" + i + "] = " + y[i] + " is negative");
                return false;
            }
        }

        // check that yA >= c
        for (int j = 0; j < N; j++) {
            double sum = 0.0;
            for (int i = 0; i < M; i++) {
                sum += A[i][j] * y[i];
            }
            if (sum < c[j] - EPSILON) {
                Log.e(TAG, "not dual feasible");
                Log.e(TAG, "c[" + j + "] = " + c[j] + ", sum = " + sum);
                return false;
            }
        }
        return true;
    }

    // check that optimal value = cx = yb
    private boolean isOptimal(double[] b, double[] c) {
        double[] x = primal();
        double[] y = dual();
        double value = value();

        // check that value = cx = yb
        double value1 = 0.0;
        for (int j = 0; j < x.length; j++)
            value1 += c[j] * x[j];
        double value2 = 0.0;
        for (int i = 0; i < y.length; i++)
            value2 += y[i] * b[i];
        if (Math.abs(value - value1) > EPSILON || Math.abs(value - value2) > EPSILON) {
            Log.e(TAG, "value = " + value + ", cx = " + value1 + ", yb = " + value2);
            return false;
        }

        return true;
    }

    private boolean check(double[][] A, double[] b, double[] c) {
        return isPrimalFeasible(A, b) && isDualFeasible(A, c) && isOptimal(b, c);
    }

    // print tableaux
    public void show() {
        Log.e(TAG, "M = " + M);
        Log.e(TAG, "N = " + N);
        for (int i = 0; i <= M; i++) {
            for (int j = 0; j <= M + N; j++) {
                Log.e("Print Tableaux", String.valueOf(a[i][j]));
            }

        }
        Log.e(TAG, "value = " + value());
        for (int i = 0; i < M; i++)
            if (basis[i] < N) Log.e(TAG, "x_" + basis[i] + " = " + a[i][M + N]);

    }


}
