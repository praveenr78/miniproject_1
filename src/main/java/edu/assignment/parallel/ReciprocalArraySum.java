package edu.assignment.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Class wrapping methods for implementing reciprocal array sum in parallel.
 */
public final class ReciprocalArraySum {

    /**
     * Default constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Sequentially compute the sum of the reciprocal values for a given array.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * Computes the size of each chunk, given the number of chunks to create
     * across a given number of elements.
     *
     * @param nChunks   The number of chunks to create
     * @param nElements The number of elements to chunk across
     * @return The default chunk size
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Integer ceil
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Computes the inclusive element index that the provided chunk starts at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the start of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The inclusive index that this chunk starts at in the set of
     * nElements
     */
    private static int getChunkStartInclusive(final int chunk,
                                              final int nChunks,
                                              final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Computes the exclusive element index that the provided chunk ends at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the end of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The exclusive end index for this chunk
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
                                            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * Task created to perform reciprocal array sum in parallel.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {

        private final int SEQ_THRESHOLD = 100000;
        /**
         * Starting index for traversal done by this task.
         */
        private final int startIndexInclusive;
        /**
         * Ending index for traversal done by this task.
         */
        private final int endIndexExclusive;
        /**
         * Input array to reciprocal sum.
         */
        private final double[] input;
        /**
         * Intermediate value produced by this task.
         */
        private double value;

        /**
         * Constructor.
         *
         * @param setStartIndexInclusive Set the starting index to begin
         *                               parallel traversal at.
         * @param setEndIndexExclusive   Set ending index for parallel traversal.
         * @param setInput               Input values
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive,
                               final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Getter for the value produced by this task.
         *
         * @return Value produced by this task
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {

            if (endIndexExclusive - startIndexInclusive <= SEQ_THRESHOLD) {

                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    // Compute sum of reciprocals of array elements.
                    value += 1 / input[i];
                }
                ;
            } else {
//                Divide & Conquer
                int midIndex = (startIndexInclusive + endIndexExclusive) / 2;

                ReciprocalArraySumTask leftSumTask = new ReciprocalArraySumTask(startIndexInclusive,
                        midIndex, input);
                ReciprocalArraySumTask rightSumTask = new ReciprocalArraySumTask(midIndex,
                        endIndexExclusive, input);

                invokeAll(leftSumTask, rightSumTask);

                value = leftSumTask.getValue() + rightSumTask.getValue();
            }
        }
    }

    /**
     * TODO: Modify this method to compute the same reciprocal sum as
     * seqArraySum, but use two tasks running in parallel under the Java Fork
     * Join framework. You may assume that the length of the input array is
     * evenly divisible by 2.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        double sum = 0;
        sum = parManyTaskArraySum(input, 2);
        return sum;
    }

    /**
     *  To implement parArraySum to use a set
     * number of tasks to compute the reciprocal array sum. 
     *
     * @param input    Input array
     * @param numTasks The number of tasks to create
     * @return The sum of the reciprocals of the array input
     */
    protected static double parManyTaskArraySum(final double[] input,
                                                final int numTasks) {
        double sum = 0;
        ForkJoinPool arraySumPool = new ForkJoinPool(numTasks);

        // Compute sum of reciprocals of array elements
        int chunkSize = getChunkSize(numTasks, input.length);

        List<ReciprocalArraySumTask> reciprocalArraySumTaskList = new ArrayList<>();

        //
        for (int t = 0; t < numTasks; t++) {
            reciprocalArraySumTaskList.add(newRecpSumTask(t, numTasks, input));
        }

        //invoke.
        reciprocalArraySumTaskList.stream().forEach(task -> {
            arraySumPool.invoke(task);
        });

        // Sum up values from tasks
        sum = reciprocalArraySumTaskList.stream().mapToDouble(sumTask -> sumTask.getValue()).sum();

     //   System.out.println("This sum is - (" + sum + ") - was processed by " + Thread.currentThread().getName());

        return sum;
    }


    /**
     * Create a new Reciprocal Summary Task for each chunk.
     *
     * @param chunk the index position of chunk.
     * @param nChunk max chunks to be processed
     * @param input the data array to compute reciprocal sum.
     * @return
     */
    private static ReciprocalArraySumTask newRecpSumTask(final int chunk,
                                                     final int nChunk,
                                                     final double[] input) {
        return new ReciprocalArraySumTask(getChunkStartInclusive(chunk, nChunk, input.length),
                getChunkEndExclusive(chunk, nChunk, input.length), input);

    }
}
