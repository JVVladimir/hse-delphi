package ru.hse.delphi.algo;

import java.util.Arrays;

public class MedianCounter {

    public static double countMedian(double[] elements) {
        Arrays.sort(elements);
        var length = elements.length;
        if (length % 2 == 0)
            return ((elements[length / 2] + elements[length / 2 - 1]) / 2.0);
        return elements[length / 2];
    }

}
