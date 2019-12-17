package ru.hse.delphi.algo;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DelphiMethod {

    private final static double THRESHOLD = 1.6;
    private static double K0 = -1;

    // Получение начального значения квантиля
    public static double getQuantile(int n, double[] votes) {
        Arrays.sort(votes); // сортируем оценки экспертов по возрастанию
        if ((n + 1) % 4 == 0) { // Если чисел нечётное кол-во
            int nk1, nk2;
            nk1 = (n + 1) / 4;
            nk2 = 3 * (n + 1) / 4;
            return votes[nk2 - 1] - votes[nk1 - 1];
        } else { // Если чисел чётное кол-во
            double a, b, c, k1, k2;
            a = (n + 1) / 4.0;
            b = votes[(int) a - 1];
            c = votes[(int) Math.ceil(a) - 1];
            k1 = b + (c - b) * 0.6;
            a = 3 * (n + 1) / 4.0;
            b = votes[(int) a - 1];
            c = votes[(int) Math.ceil(a) - 1];
            k2 = b + (c - b) * 0.6;
            return k2 - k1;
        }
    }

    public static void setInitQuantile(int n, double[] votes) {
        K0 = getQuantile(n, votes);
    }

    public static double getK0() {
        return K0;
    }

    // Проврека, что текущий квантиль на 1.6 больше начального квантиля
    boolean quantileIsNotThreshold(double curK) {
        return curK > K0 / THRESHOLD;
    }

}
