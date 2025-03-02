package prog11;

import java.util.*;

public class NotGPT implements SearchEngine {

    HardDisk pageDisk = new HardDisk();

    HardDisk wordDisk = new HardDisk();

    Map<String, Long> urlToIndex = new TreeMap<>();

    Map<String, Long> wordToIndex = new HashMap<>();


    /**
     * Write an indexPage method that takes the String url of the web page
     * as input and returns the index of its newly created page file.  It
     * gets the index of a new file from pageDisk, creates a new
     * InfoFile, and stores the InfoFile in pageDisk.  Then it tells the
     * Map urlToIndex to map url to that index and returns the index.
     **/

    public long indexPage(String url) {
        long index = pageDisk.newFile(); //gets index of new file from page disk
        InfoFile file = new InfoFile(url);
        pageDisk.put(index, file); //stores infoFile in pageDisk
        urlToIndex.put(url, index); //maps url to that index
        System.out.println("indexing page " + index + " " + file);
        return index;
    }

    //8. Write the indexWord method.
    public long indexWord(String word) {
        long index = wordDisk.newFile();
        InfoFile file = new InfoFile(word);
        wordDisk.put(index, file);
        wordToIndex.put(word, index);
        System.out.println("indexing word " + index + " " + file);
        return index;
    }


    @Override
    public void collect(Browser browser, List<String> startingURLs) {

        //In the NotGPT collect method, create a queue of page indices.
        ArrayDeque<Long> pageIndexes = new ArrayDeque<>();
        System.out.println("starting pages " + startingURLs);

        //For each starting URL,
        for (int i = 0; i < startingURLs.size(); i++) {

            //check if it has been indexed already (how?).
            if (urlToIndex.get(startingURLs.get(i)) == null) {
                //If not, index it using indexPage and put the new index into the queue.
                pageIndexes.offer(indexPage(startingURLs.get(i)));
            }

        }
        //Do the following while the queue is not empty:
        while (!pageIndexes.isEmpty()) {

            System.out.println("queue " + pageIndexes);

            //Dequeue a page index.
            Long pageIndex = pageIndexes.poll();

            //Load its URL into the browser.
            InfoFile URL = pageDisk.get(pageIndex);
            System.out.println("dequeued " + URL);

            boolean loads = browser.loadPage(URL.data);

            //If it loads
            if (loads) {

                //Use a Set<String> to determine if a URL has been seen before on that page.
                //For each URL on the page, add its index to the list of indices for
                //the InfoFile, but only the first time you see it.  Test.
                Set<String> seenURLs = new HashSet<>();
                Set<String> seenWords = new HashSet<>();

                //get its list of URLs.
                List<String> urlList = browser.getURLs();
                //get its list of Words.
                List<String> wordList = browser.getWords();

                System.out.println("urls " + urlList);

                for (String url : urlList) {

                    if (!seenURLs.contains(url)) {
                        seenURLs.add(url);
                        Long index = urlToIndex.get(url);

                        if (index == null) {
                            index = indexPage(url);
                            pageIndexes.add(index);
                        }
                        URL.indices.add(index);
                    }


                }
                pageDisk.put(pageIndex, URL);
                System.out.println("updated page file " + pageDisk.get(pageIndex));


                System.out.println("words " + wordList);
                for (String word : wordList) {
                    if (!seenWords.contains(word)) {
                        seenWords.add(word);
                        Long index = wordToIndex.get(word);
                        if (index == null) {
                            index = indexWord(word);
                        }
                        wordDisk.get(index).indices.add(pageIndex);
                        System.out.println("updated word file " + wordDisk.get(index));
                    }

                }


            }
        }

    }

    private class Vote implements Comparable<Vote>{
        Long index;
        double vote;

        Vote(Long index, double vote) {
            this.index = index;
            this.vote = vote;
        }

        public int compareTo(Vote other){
            int indexComparison = Long.compare(this.index, other.index);

            if(indexComparison!=0){
                return indexComparison;
            }

            return Double.compare(this.vote, other.vote);
        }
    }


    @Override
    public void rank(boolean fast) {

        //count the number of pages that have no links to other pages.
        double count = 0.0;

        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.influence = 1.0;
            file.influenceTemp = 0.0;

            //if page has no links, it has empty indices list
            InfoFile pageInfo = entry.getValue();
            if (pageInfo.indices.isEmpty()) {
                count++;

            }

        }



        //Set double defaultInfluence to 1.0 * count / pageDisk.size()
        //   instead of 0.0.
        double defaultInfluence = 1.0 * count / pageDisk.size();



        if (!fast) {
            for (int j = 0; j < 20; j++) {
                rankSlow(defaultInfluence);
            }
        } else {
            for (int j = 0; j < 20; j++) {
                rankFast(defaultInfluence);
            }

        }

    }


    @Override
    public String[] search(List<String> searchWords, int numResults) {


        // 11.3 STEP 5
        // Inside search, declare and initialize a PageComparator variable pageComparator.
        // Declare a variable inside search:
        // Matching pages with the least popular page on the top of the queue.
        // PriorityQueue<Long> bestPageIndices;
        // Initialize bestPageIndices using PriorityQueue with pageComparator as its Comparator.
        PageComparator pageComparator = new PageComparator();
        PriorityQueue<Long> bestPageIndices = new PriorityQueue<>(pageComparator);

        // // 11.3 STEP 1
        // Inside the NotGPT search method, create two variables:
        // Iterator into list of page indices for each key word.
        // Iterator<Long>[] wordPageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];
        // Current page index in each list, just ``behind'' the iterator. long[] currentPageIndex;

        Iterator<Long>[] wordPageIndexIterators = new Iterator[searchWords.size()];

        long[] currentPageIndex = new long[searchWords.size()]; // need to initialize this?

        // 11.3 STEP 1
        // Write a loop to initialize the entries of wordPageIndexIterators. (store iterators in that list)
        // wordPageIndexIterators[i] should be set to an iterator over page indices of the word searchWords[i].
        // How to go from word to infoFile?
        // word to index (long)
        // worddisk on that long to infoFile
        // once in infoFile, call iterator

        for (int i = 0; i < searchWords.size(); i++) {
            String word = searchWords.get(i);

            if (wordToIndex.containsKey(word)) {
                long wordIndex = wordToIndex.get(word);
                InfoFile wordFile = wordDisk.get(wordIndex);

                wordPageIndexIterators[i] = wordFile.indices.iterator();
            } else {
                // If a keyword is not found, return an empty array, do I need this?
                return new String[0];
            }
        }

        // 11.3 STEP 1
        // Initialize currentPageIndex.  You just have to allocate the array.
        // You don't have to write a loop to initialize the elements of the array because all its elements will automatically be zero.
        // 11.3 STEP 4
        // Implement the loop of search.
        // While getNextPageIndices is true check if the entries of currentPageIndex are all equal.
        // If so, you have a found a match. Print out its URL.

        PriorityQueue<Vote> resultQueue = new PriorityQueue<>();

        while (getNextPageIndices(currentPageIndex, wordPageIndexIterators)) {
            if (allEqual(currentPageIndex)) {
                long matchingPageIndex = currentPageIndex[0];
                double influence = pageDisk.get(matchingPageIndex).influence;
                String matchingURL = pageDisk.get(matchingPageIndex).data;

                resultQueue.add(new Vote(matchingPageIndex, influence));

                System.out.println(matchingURL); // printed out matchingURL so continue to next step

                // 11.3 STEP 6
                // When you find a matching page, after you print it out, do the following.
                // If the priority queue is not "full" (has numResults elements), just offer the matching page index.
                // If the priority queue is full, use peek() and pageComparator to determine if matching page should go into the queue.
                // If so, do a poll() before the offer.*/

                if (bestPageIndices.size() < numResults) {
                    bestPageIndices.offer(matchingPageIndex);
                } if (bestPageIndices.size() == numResults) {
                    // If the priority queue is full, check if the matching page should go into the queue
                    Long smallestInfluencePageIndex = bestPageIndices.peek();
                    if (pageComparator.compare(matchingPageIndex, smallestInfluencePageIndex) > 0) {
                        // If the matching page has higher influence than the smallest influence in the queue
                        bestPageIndices.poll();
                        bestPageIndices.offer(matchingPageIndex);
                    }
                }
                System.out.println(matchingURL);

            }
        } // end of all priority queue stuff


        // 11.3 STEP 7
        // Create an array of String which will hold the results.  How big should it be?
        // Unload the priority queue into the string.
        // But polling the queue gives you the pages in reverse order from least significant to most significant.
        // What should you do?

        int resultCount = Math.min(numResults, bestPageIndices.size());
        String[] results = new String[resultCount];


        for (int i= resultCount; i>0; i--) {
            long pageIndex = bestPageIndices.poll();
            results[i - 1] = pageDisk.get(pageIndex).data;
            ;
        }




        return results;



        /*

        PageComparator pageComparator = new PageComparator();

        PriorityQueue<Long> bestPageIndices = new PriorityQueue<>(pageComparator);

        // Current page index in each list, just ``behind'' the iterator.
        long[] currentPageIndex = new long[ searchWords.size()];



        // Iterator into list of page indices for each key word.
        Iterator<Long>[] wordPageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];



        //Write a loop to initialize the entries of wordPageIndexIterators.
        //   wordPageIndexIterators[i] should be set to an iterator over page
        //   indices of the word searchWords[i].
        for (int i = 0; i < searchWords.size(); i++) {
            wordPageIndexIterators[i] = wordDisk.get(wordToIndex.get(searchWords.get(i))).indices.iterator();

        }

        while(getNextPageIndices(currentPageIndex, wordPageIndexIterators)){
            if(allEqual(currentPageIndex)){
                System.out.println(pageDisk.get(currentPageIndex[0]).data);

                if(bestPageIndices.size()< numResults) {
                    bestPageIndices.offer(currentPageIndex[0]);
                } else{
                    if(pageComparator.compare(bestPageIndices.peek(), currentPageIndex[0]) < 0){
                        bestPageIndices.poll();
                        bestPageIndices.offer(currentPageIndex[0]);
                    }
                }


                    System.out.println(pageDisk.get(currentPageIndex[0]).data);

            }
        }

        int resultCount = Math.min(numResults, bestPageIndices.size());
        String[] results = new String[resultCount];

        int i = 0;
        while (i < resultCount) {
            long pageIndex = bestPageIndices.poll();
            results[i] = pageDisk.get(pageIndex).data;
            i++;
        }

        return results;






        //return new String[0];

         */


    }

    /** Check if all elements in an array of long are equal.
     @param array an array of numbers
     @return true if all are equal, false otherwise
     */
    private boolean allEqual (long[] array) {
        for(int i = 1; i< array.length; i++){
            if(array[i] != array[i-1]){
                return false;
            }
        }
        return true;
    }

    /** Get the largest element of an array of long.
     @param array an array of numbers
     @return largest element
     */
    private long getLargest (long[] array) {
        long largest = array[0];
        for(int i= 1; i< array.length; i++){
            if(array[i] > array[i-1]){
                largest = array[i];
            }
        }
        return largest;
    }

    /** If all the elements of currentPageIndex are equal,
     set each one to the next() of its Iterator,
     but if any Iterator hasNext() is false, just return false.

     Otherwise, do that for every element not equal to the largest element.

     Return true.

     @param currentPageIndex array of current page indices
     @param wordPageIndexIterators array of iterators with next page indices
     @return true if all page indices are updated, false otherwise
     */
    private boolean getNextPageIndices
    (long[] currentPageIndex, Iterator<Long>[] wordPageIndexIterators) {
        if(allEqual(currentPageIndex)){
            for(int i = 0; i<currentPageIndex.length; i++){
                if(wordPageIndexIterators[i].hasNext()){
                    currentPageIndex[i] = wordPageIndexIterators[i].next();
                }else{
                    return false;
                }
            }
        }
        else{
            long largest = getLargest(currentPageIndex);
            for(int i =0; i<currentPageIndex.length; i++){
                if((currentPageIndex[i] != largest)){
                    if(wordPageIndexIterators[i].hasNext()){
                        currentPageIndex[i] = wordPageIndexIterators[i].next();
                    }else{
                        return false;
                    }
                }
            }
        }
        return true;
    }




        void rankSlow(double defaultInfluence) {
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) { //visit each page file
            long pageIndex = entry.getKey();
            InfoFile pageInfo = entry.getValue();

            double influencePerIndex = pageInfo.influence / pageInfo.indices.size();//amount of influence divided by amount sending influence to

            for (long index : pageInfo.indices) { //for each index on page
                InfoFile indexedPage = pageDisk.get(index);
                indexedPage.influenceTemp += influencePerIndex; //adding influence gotten to influenceTemp
            }
        }
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long pageIndex = entry.getKey();
            InfoFile pageInfo = entry.getValue();

            pageInfo.influence = pageInfo.influenceTemp + defaultInfluence;
            pageInfo.influenceTemp = 0.0;
        }

    }


     void rankFast(double defaultInfluence) { //double defaultInfluence

        //create an empty List of Vote
        List<Vote> votes = new ArrayList<>();

        //For each page file and
        //   for each index on that page file, create a Vote and add it to the
        //   list.
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long pageIndex = entry.getKey();
            InfoFile pageInfo = entry.getValue();

            double influencePerIndex = pageInfo.influence / pageInfo.indices.size();

            for(long index: pageInfo.indices){
                votes.add(new Vote(index, influencePerIndex));
            }
        }

         //Sort the list of votes:
         Collections.sort(votes);

        //Create and initialize an iterator variable for the votes.
         Iterator<Vote> iterator = votes.iterator();

         //For each page, add the influence of the votes that have the same
         //   index as the page.

         Vote currentVote = null;

         for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {

             long pageIndex = entry.getKey();
             InfoFile pageInfo = entry.getValue();

             pageInfo.influence = 0.0;//temp

             if(currentVote != null && currentVote.index== pageIndex ){
                 pageInfo.influence += currentVote.vote;//temp
             }



             while(iterator.hasNext()){

                 Vote vote = iterator.next();

                 if(vote.index == pageIndex){
                     pageInfo.influence += vote.vote;//temp
                 }else if(vote.index > pageIndex){
                     currentVote = vote;

                     break;
                 }

             }



         }

         // Update the influence scores for each page.
         for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
             long pageIndex = entry.getKey();
             InfoFile pageInfo = entry.getValue();

             pageInfo.influence = pageInfo.influence + defaultInfluence;//temp+

         }


}

//Inside the NotGPT class create a PageComparator class such that its
//
//	compare(pageIndex1, pageIndex2)
//
//   method returns a value < 0, = 0, > 0 depending the the comparison
//   of the influence of the pages with those page indices.  Use the
//   Double.compare method to implement it.

    public class PageComparator implements Comparator<Long>{
        // compare(pageIndex1, pageIndex2) method
        @Override
        public int compare(Long pageIndex1, Long pageIndex2) {
            // Get the influence values for the given page indices
            double influence1 = pageDisk.get(pageIndex1).influence;
            double influence2 = pageDisk.get(pageIndex2).influence;

            // Use Double.compare to compare the influence values
            return Double.compare(influence1, influence2);



            //public int compare (Object pageIndex1, Object pageIndex2){
            //return Double.compare(wordDisk.get(pageIndex1).influence, wordDisk.get(pageIndex2).influence);
        }
    }


}


























