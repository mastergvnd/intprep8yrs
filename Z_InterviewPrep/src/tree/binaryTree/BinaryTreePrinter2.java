package tree.binaryTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BinaryTreePrinter2 {
    public static <T extends Comparable<?>> void printNode2(Node2 root) {
        int maxLevel = BinaryTreePrinter2.maxLevel(root);

        printNode2Internal(Collections.singletonList(root), 1, maxLevel);
    }

    private static <T extends Comparable<?>> void printNode2Internal(List<Node2> nodes, int level, int maxLevel) {
        if (nodes.isEmpty() || BinaryTreePrinter2.isAllElementsNull(nodes))
            return;

        int floor = maxLevel - level;
        int endgeLines = (int) Math.pow(2, (Math.max(floor - 1, 0)));
        int firstSpaces = (int) Math.pow(2, (floor)) - 1;
        int betweenSpaces = (int) Math.pow(2, (floor + 1)) - 1;

        BinaryTreePrinter2.printWhitespaces(firstSpaces);

        List<Node2> newNode2s = new ArrayList<Node2>();
        for (Node2 node : nodes) {
            if (node != null) {
                System.out.print(node.getData());
                newNode2s.add(node.getLeftChild());
                newNode2s.add(node.getRightChild());
            } else {
                newNode2s.add(null);
                newNode2s.add(null);
                System.out.print(" ");
            }

            BinaryTreePrinter2.printWhitespaces(betweenSpaces);
        }
        System.out.println("");

        for (int i = 1; i <= endgeLines; i++) {
            for (int j = 0; j < nodes.size(); j++) {
                BinaryTreePrinter2.printWhitespaces(firstSpaces - i);
                if (nodes.get(j) == null) {
                    BinaryTreePrinter2.printWhitespaces(endgeLines + endgeLines + i + 1);
                    continue;
                }

                if (nodes.get(j).getLeftChild() != null)
                    System.out.print("/");
                else
                    BinaryTreePrinter2.printWhitespaces(1);

                BinaryTreePrinter2.printWhitespaces(i + i - 1);

                if (nodes.get(j).getRightChild() != null)
                    System.out.print("\\");
                else
                    BinaryTreePrinter2.printWhitespaces(1);

                BinaryTreePrinter2.printWhitespaces(endgeLines + endgeLines - i);
            }

            System.out.println("");
        }

        printNode2Internal(newNode2s, level + 1, maxLevel);
    }

    private static void printWhitespaces(int count) {
        for (int i = 0; i < count; i++)
            System.out.print(" ");
    }

    private static <T extends Comparable<?>> int maxLevel(Node2 node) {
        if (node == null)
            return 0;

        return Math.max(BinaryTreePrinter2.maxLevel(node.getLeftChild()), BinaryTreePrinter2.maxLevel(node.getRightChild())) + 1;
    }

    private static <T> boolean isAllElementsNull(List<T> list) {
        for (Object object : list) {
            if (object != null)
                return false;
        }

        return true;
    }
}
