/* CS314 LINKED LIST VIEWER COMPATIBLE TEST FILE
 * Copyright (c) 2020 Michael Scott, Andrew Smith.
 * All rights reserved.
 *
 * This file is a slightly modified version of the default LinkedList test file provided with the assignment, adapted to
 * work with CS314-LinkedListView.  There should be no behavioural changes between the two insofar as the LinkedList
 * itself is concerned, but if your implementation of LinkedList::makeEmpty() (test 0.7) is not working, then you may
 * encounter unexpected results.
 */

import java.util.Arrays;
import java.util.Iterator;

public class MikeTest {
    public static void main(String[] args) {
        Object[] actual;
        Object[] expected;

        try (LinkedListView<String> list = new LinkedListView<>("MikeTest.html")) {
            //test 0
            System.out.println("\nTest 0: initial list is empty");
            if( list.toString().equals("[]") )
                System.out.println("Passed test 0");
            else
                System.out.println("Failed test 0");

            //test 0.1
            System.out.println("\nTest 0.1: add to end");
            list.add("A");
            if( list.get(0).equals("A") )
                System.out.println("Passed test 0.1");
            else
                System.out.println("Failed test 0.1");

            //test 0.2
            System.out.println("\nTest 0.2: size");
            if( list.size() == 1 )
                System.out.println("Passed test 0.2");
            else
                System.out.println("Failed test 0.2");

            //test 0.3
            System.out.println("\nTest 0.3: remove from position 0");
            String removed = list.remove(0);
            if(removed.equals("A"))
                System.out.println("Passed test 0.31");
            else
                System.out.println("Failed test 0.31");

            System.out.println("\nTest 0.31: toStrin after remove");
            //test 0.31
            if( list.toString().equals("[]") )
                System.out.println("Passed test 0.3");
            else
                System.out.println("Failed test 0.3");

            //test 0.4
            System.out.println("\nTest 0.4: size");
            if( list.size() == 0 )
                System.out.println("Passed test 0.4");
            else
                System.out.println("Failed test 0.4");

            //test 0.5
            System.out.println("\nTest 0.5: add and toString");
            list.add("A");
            list.add("B");
            if( list.toString().equals("[A, B]") )
                System.out.println("Passed test 0.5");
            else
                System.out.println("Failed test 0.5");

            //test 0.6
            System.out.println("\nTest 0.6: size");
            if( list.size() == 2 )
                System.out.println("Passed test 0.6");
            else
                System.out.println("Failed test 0.6");

            //test 0.7
            System.out.println("\nTest 0.7: makeEmpty");
            list.makeEmpty();
            if( list.size() == 0 )
                System.out.println("Passed test 0.7");
            else
                System.out.println("Failed test 0.7");

            //test 0.8
            System.out.println("\nTest 0.8: makeEmpty on empty list");
            list.makeEmpty();
            if( list.size() == 0 )
                System.out.println("Passed test 0.8");
            else
                System.out.println("Failed test 0.8");

            //test 1
            System.out.println("\nTest 1: Adding at end");
            list.add("A");
            actual = toArray(list);
            expected = new Object[]{"A"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 1");
            else
                System.out.println("Failed test 1");

            //test 2
            System.out.println("\nTest 2: making empty");
            list.makeEmpty();
            actual = toArray(list);
            expected = new Object[] {};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 2");
            else
                System.out.println("Failed test 2");


            //test 3
            System.out.println("\nTest 3: Adding at pos 0 in empty list");
            list.insert(0, "A");
            actual = toArray(list);
            expected = new Object[] {"A"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 3");
            else
                System.out.println("Failed test 3");



            //test 4
            System.out.println("\nTest 4: Adding at front");
            list.makeEmpty();
            list.addFirst("A");
            actual = toArray(list);
            expected = new Object[] {"A"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 4");
            else
                System.out.println("Failed test 4");



            //test 5
            System.out.println("\nTest 5: Removing from front");
            list.removeFirst();
            actual = toArray(list);
            expected = new Object[] {};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 5");
            else
                System.out.println("Failed test 5");


            //test 6
            list.makeEmpty();
            System.out.println("\nTest 6: Adding at end");
            list.addLast("A");
            actual = toArray(list);
            expected = new Object[] {"A"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 6");
            else
                System.out.println("Failed test 6");


            //test 7
            System.out.println("\nTest 7: Removing from back");
            list.removeLast();
            actual = toArray(list);
            expected = new Object[] {};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 7");
            else
                System.out.println("Failed test 7");

            //test 8
            System.out.println("\nTest 8: Adding at middle");
            list.makeEmpty();
            list.add("A");
            list.add("C");
            list.insert(1, "B");
            actual = toArray(list);
            expected = new Object[] {"A", "B", "C"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 8");
            else
                System.out.println("Failed test 8");


            //test 9
            System.out.println("\nTest 9: Setting");
            list.makeEmpty();
            list.add("A");
            list.add("D");
            list.add("C");
            String oldValue = list.set(1, "B");
            actual = toArray(list);
            expected = new Object[] {"A", "B", "C"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 9.1");
            else
                System.out.println("Failed test 9.1");
            if( oldValue.equals("D") )
                System.out.println("Passed test 9.2");
            else
                System.out.println("Failed test 9.2");


            //test 10
            System.out.println("\nTest 10: Removing");
            list.makeEmpty();
            list.add("A");
            list.add("B");
            list.add("C");
            list.add("D");
            list.remove(0);
            list.remove( list.size() - 1 );
            actual = toArray(list);
            expected = new Object[] {"B", "C"};
            System.out.println( "Expected result: " + Arrays.toString(expected) );
            System.out.println( "Actual result: " + Arrays.toString(actual) );
            if( arraysSame(actual, expected) )
                System.out.println("Passed test 10");
            else
                System.out.println("Failed test 10");
        } catch (NoSuchFieldException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    private static Object[] toArray(LinkedList<String> list) {
        Object[] result = new Object[list.size()];
        Iterator<String> it = list.iterator();
        int index = 0;
        while( it.hasNext() ){
            result[index] = it.next();
            index++;
        }
        return result;
    }

    //pre: none
    private static boolean arraysSame(Object[] one, Object[] two)  {
        boolean same;
        if( one == null || two == null ) {
            same = (one == two);
        }
        else {
            //neither one or two are null
            assert one != null && two != null;
            same = one.length == two.length;
            if( same ) {
                int index = 0;
                while( index < one.length && same ) {
                    same = ( one[index].equals(two[index]) );
                    index++;
                }
            }
        }
        return same;
    }
}
