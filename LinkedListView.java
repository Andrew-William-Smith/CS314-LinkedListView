/* CS 314 LINKED LIST VIEWER
 * Copyright (c) 2020 Andrew Smith.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * A version of the CS 314 LinkedList class that supports visualisation.  Must be instantiated in a try-with-resources
 * block; CS 314 students, that means you need to instantiate this class like this:
 * <pre>
 * try (LinkedListView<String> list = new LinkedListView<>("TestOutput.html")) {
 *     // Do your work
 * } catch (NoSuchFieldException e) {
 *     // Handle the issue
 * }
 * </pre>
 *
 * @param <E> The type of object to store in this list.
 * @see LinkedList
 */
public class LinkedListView<E> extends LinkedList<E> implements AutoCloseable {
    //region Instance variables
    /** Common names of LinkedList header nodes. */
    private static final String[] HEAD_NAMES = {"begin", "first", "front", "head", "init"};
    /** Header node of the LinkedList class. */
    private Field headNodeField;
    /** Common names of LinkedList trailer nodes. */
    private static final String[] TAIL_NAMES = {"end", "final", "last", "tail", "trail"};
    /** Tail node of the LinkedList class; null if LinkedList is circular. */
    private Field tailNodeField;

    /** Reference from a LinkedList node to the previous node. */
    private Field nodePrevField;
    /** Field containing the data of a LinkedList node. */
    private Field nodeDataField;
    /** Reference from a LinkedList node to the next node. */
    private Field nodeNextField;

    /** Global logger for status monitoring. */
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    /** Writer to specified output HTML file. */
    private FileWriter htmlWriter;

    /** Formatter for graph timestamps. */
    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    /** Preamble for HTML graph output files.  Must be formatted with timestamps.  Also, string blocks please? */
    private static final String HTML_PREAMBLE =
            "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<title>LinkedList operation transcript at %s</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "<script src=\"//d3js.org/d3.v4.min.js\"></script>\n"
            + "<script src=\"https://unpkg.com/viz.js@1.8.0/viz.js\"></script>\n"
            + "<script src=\"https://unpkg.com/d3-graphviz@0.1.2/build/d3-graphviz.js\"></script>\n"
            + "<h1><code>LinkedList</code> operation transcript &mdash; %s</h1>";
    /** Closing statements for HTML graph output files. */
    private static final String HTML_POSTAMBLE = "</body>\n</html>";
    /**
     * List operation template.  Must be formatted with:
     * <ul>
     *     <li>Header tag name</li>
     *     <li>Operation name</li>
     *     <li>Timestamp</li>
     *     <li>Header tag name</li>
     * </ul>
     */
    private static final String OPERATION_PREAMBLE =
            "<%s><code>%s</code> at %s</%s>\n";
    /** Diagramme template.  Must be formatted with a UUID twice. */
    private static final String DIAGRAMME_PREAMBLE =
            "<div id=\"%s\"></div>\n"
            + "<script>\n"
            + "d3.select('[id=\"%s\"]').graphviz().renderDot(`\n"
            + "strict digraph {\n"
            + "  node[shape=record];\n"
            + "  rankdir=LR;\n";
    private static final String DIAGRAMME_POSTAMBLE = "}`);\n</script>\n";
    //endregion

    //region Constructor and field resolution
    /**
     * Construct a new LinkedListView instance, using reflection to determine how the LinkedList is structured.
     * @param fileName The name of the file to which to write HTML list diagrams.
     * @throws NoSuchFieldException If a header node could not be found in LinkedList.
     */
    public LinkedListView(String fileName) throws NoSuchFieldException {
        // Find the header node, which must exist
        this.headNodeField = this.findFieldName(LinkedList.class, HEAD_NAMES);
        if (this.headNodeField == null) {
            // The list must have a valid header
            throw new NoSuchFieldException("Unable to find LinkedList header node.");
        }
        this.headNodeField.setAccessible(true);
        LOGGER.info("Header node name: \"" + this.headNodeField.getName() + "\"");

        // Find the optional tail node
        this.tailNodeField = this.findFieldName(LinkedList.class, TAIL_NAMES);
        if (this.tailNodeField == null) {
            LOGGER.info("No tail node found; assuming list to be circular.");
        } else {
            this.tailNodeField.setAccessible(true);
            LOGGER.info("Tail node name: \"" + this.tailNodeField.getName() + "\"");

            // Head and tail must have same type
            if (!this.headNodeField.getType().equals(this.tailNodeField.getType())) {
                throw new NoSuchFieldException("Head and tail nodes must have the same type.");
            }
        }

        // Find required LinkedList node fields
        this.nodePrevField = this.resolveNodeField("prev");
        this.nodeDataField = this.resolveNodeField("data");
        this.nodeNextField = this.resolveNodeField("next");

        // Create file and write preamble
        try {
            this.htmlWriter = new FileWriter(fileName);
            this.writePreamble();
            this.writeOperation("LinkedList()", true);
        } catch (IOException e) {
            // Cannot proceed if the file cannot be written
            LOGGER.severe("Failed to create output file: " + e.getMessage());
            System.exit(-1);
        }
    }

    @Override
    public void close() {
        try {
            htmlWriter.write(HTML_POSTAMBLE);
            htmlWriter.close();
        } catch (IOException e) {
            // If we can't close the file, something has gone horribly wrong
            LOGGER.severe("Failed to close output file: " + e.getMessage());
        }
    }

    /**
     * Find the field in the specified class with a partial name in the specified list.
     *
     * @param searchClass The class in which to find the desired field.
     * @param names The list of possible names for the desired field, with all names in lowercase.
     * @return The field with a specified name if it exists, or null if it does not.
     */
    private Field findFieldName(Class<?> searchClass, String[] names) {
        for (Field field : searchClass.getDeclaredFields()) {
            String fieldName = field.getName().toLowerCase();
            for (String name : names) {
                // Search for substrings to allow for name variations
                if (fieldName.contains(name)) {
                    return field;
                }
            }
        }

        return null;
    }

    /**
     * Resolve the specified name to a field in the LinkedList node class and make it accessible.
     *
     * @param name The name of the field to resolve.
     * @return The resolved field.
     * @throws NoSuchFieldException If the field could not be resolved.
     */
    private Field resolveNodeField(String name) throws NoSuchFieldException {
        Field field = this.findFieldName(headNodeField.getType(), new String[] {name});
        if (field == null) {
            throw new NoSuchFieldException("Unable to resolve LinkedList node field with name \"" + name + "\"");
        }

        field.setAccessible(true);
        LOGGER.info("LinkedList node field \"" + name + "\" declared name: \"" + field.getName() + "\"");
        return field;
    }
    //endregion

    //region File I/O utilities
    /**
     * Write the preamble of a HTML graph document to htmlWriter.
     */
    private void writePreamble() throws IOException {
        String startTimestamp = this.currentTimestamp();
        htmlWriter.write(String.format(HTML_PREAMBLE, startTimestamp, startTimestamp));
    }

    /**
     * @return A formatted representation of the current date and time.
     */
    private String currentTimestamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }

    /**
     * Write the effect of the specified operation to htmlWriter.
     * @param operationName The name of the operation of which to write the effect.
     * @param writeDiagramme Whether or not to draw a diagramme for this operation.
     */
    private void writeOperation(String operationName, boolean writeDiagramme) {
        // Only write if the operation was called from outside of LinkedList
        final int callerStackOffset = 3;
        if (Thread.currentThread().getStackTrace()[callerStackOffset].getClassName().equals("LinkedList")) {
            return;
        }

        // Create HTML structure for operation
        String operationId = UUID.randomUUID().toString();
        String headerTag = writeDiagramme ? "h2" : "h4";

        try {
            htmlWriter.write(String.format(OPERATION_PREAMBLE, headerTag, operationName, this.currentTimestamp(),
                    headerTag));
            if (writeDiagramme) {
                htmlWriter.write(String.format(DIAGRAMME_PREAMBLE, operationId, operationId));
                this.writeDiagramme();
                htmlWriter.write(DIAGRAMME_POSTAMBLE);
            }
            LOGGER.info("Logged operation " + operationName);
        } catch (IOException | IllegalAccessException e) {
            LOGGER.severe("Operation " + operationName + ": " + e.getMessage());
        }
    }
    //endregion

    //region List rendering
    /**
     * Write a GraphViz Dot representation of this list to htmlWriter.
     */
    public void writeDiagramme() throws IllegalAccessException, IOException {
        // Store DotListNodes in a map: LinkedList node -> DotListNode
        HashMap<Object, DotListNode> dotNodes = new HashMap<>();

        final String headerNodeName = "__HEADER_NAME";
        final String tailNodeName = "__TAIL_NAME";

        // Process from header node
        this.writeExternalVariable(headerNodeName, this.headNodeField.getName());
        DotListNode headNode = this.processNode(this.headNodeField.get(this), dotNodes);
        // Process from tail node (if extant)
        DotListNode tailNode = null;
        if (this.tailNodeField != null) {
            this.writeExternalVariable(tailNodeName, this.tailNodeField.getName());
            tailNode = this.processNode(this.tailNodeField.get(this), dotNodes);
        }

        // Print nodes
        for (DotListNode node : dotNodes.values()) {
            node.writeDot();
        }

        // Print edges
        if (headNode == null) {
            this.writeNullExternalNode(headerNodeName);
        } else {
            htmlWriter.write(String.format("  %s -> %s%s;\n", headerNodeName, DotListNode.DOT_PREFIX,
                    headNode.getUUID()));
        }
        if (tailNode == null) {
            this.writeNullExternalNode(tailNodeName);
        } else {
            htmlWriter.write(String.format("  %s%s -> %s [dir=back];\n", DotListNode.DOT_PREFIX, tailNode.getUUID(),
                    tailNodeName));
        }
        htmlWriter.write("  edge[tailclip=false,arrowtail=dot,dir=both];\n");

        for (DotListNode node : dotNodes.values()) {
            node.writeDotEdges(dotNodes);
        }
    }

    /**
     * Print an external variable with the specified name to standard output as a Dot node.
     * @param nodeName The name of the Dot node to print.
     * @param name The name of the Java variable to print.
     */
    private void writeExternalVariable(String nodeName, String name) throws IOException {
        htmlWriter.write("  " + nodeName + "[style=filled,fillcolor=black,fontcolor=white,fontname=monospace,"
                + "shape=ellipse,label=\"" + name + "\"];\n");
    }

    /**
     * Draw an edge from the specified external variable's node to a null node.
     * @param nameNode The name of the node containing this variable's name.
     */
    private void writeNullExternalNode(String nameNode) throws IOException {
        htmlWriter.write("  " + nameNode + "_NULL [shape=circle,label=<<B>∅</B>>];\n");
        htmlWriter.write("  " + nameNode + " -> " + nameNode + "_NULL;\n");
    }

    /**
     * Recursively populate a map from LinkedList nodes onto DotListNodes, beginning at the specified node.
     * @param startNode The node at which to start populating the map.
     * @param nodeCache The map into which to store node mappings.
     * @return The DotListNode corresponding to startNode.
     */
    private DotListNode processNode(Object startNode, HashMap<Object, DotListNode> nodeCache)
            throws IllegalAccessException {
        // If start node is null, do nothing
        if (startNode == null) {
            return null;
        }

        // If this node has already been mapped, do not map again
        if (nodeCache.containsKey(startNode)) {
            return nodeCache.get(startNode);
        }

        // Otherwise, create a new mapping and recurse
        DotListNode mappedNode = new DotListNode(startNode);
        nodeCache.put(startNode, mappedNode);
        this.processNode(mappedNode.getPrevNode(), nodeCache);
        this.processNode(mappedNode.getNextNode(), nodeCache);

        return mappedNode;
    }

    /**
     * Lazily-processed representation of a single LinkedList node.  Instances of this class should not be persisted
     * longer than a single method due to potential changes in the structure of the LinkedList itself.
     */
    private class DotListNode {
        /** Prefix to prepend to Dot node names. */
        public static final String DOT_PREFIX = "__NODE_";

        /** The LinkedList node upon which this node is based. */
        private Object baseNode;
        /** A unique identifier for this node, used in Dot graph generation because Java hash codes can collide. */
        private UUID uuid;

        /**
         * Construct a single DotListNode from the specified field in the specified object, which should be a LinkedList
         * node.
         *
         * @param baseNode The LinkedList node on which to base this node.
         */
        public DotListNode(Object baseNode) {
            this.uuid = UUID.randomUUID();
            this.baseNode = baseNode;
        }

        /**
         * Return this node's UUID (universally unique identifier) as a Dot-compatible String.
         * @return A Dot-compatible representation of this node's UUID.
         */
        public String getUUID() {
            return Long.toUnsignedString(this.uuid.getMostSignificantBits())
                    + Long.toUnsignedString(this.uuid.getLeastSignificantBits());
        }

        /**
         * @return The LinkedList node preceding this node.
         */
        public Object getPrevNode() throws IllegalAccessException {
            return nodePrevField.get(this.baseNode);
        }

        /**
         * @return The data contained in (referenced by) the LinkedList node backing this one.
         */
        @SuppressWarnings("unchecked")
        private E getData() throws IllegalAccessException {
            return (E) nodeDataField.get(this.baseNode);
        }

        /**
         * @return The LinkedList node following this node.
         */
        public Object getNextNode() throws IllegalAccessException {
            return nodeNextField.get(this.baseNode);
        }

        /**
         * Write this node to htmlWriter as a GraphViz Dot node.
         */
        public void writeDot() throws IllegalAccessException, IOException {
            // Write UUID by integer value, as Dot does not allow hyphens in node names
            htmlWriter.write("  " + DOT_PREFIX + this.getUUID() + "[label=\"");

            // Get LinkedList node properties
            Object prevNode = this.getPrevNode();
            E data = this.getData();
            Object nextNode = this.getNextNode();

            if (prevNode == null && data == null && nextNode == null) {
                // Null node: print a simple representation of null
                htmlWriter.write("null");
            } else {
                // Node with value: print a record node
                String dataStr = (data == null) ? " null" : " " + data.toString().replaceAll("\"", "\\\"");
                htmlWriter.write("{<prev>|<data>" + dataStr + "|<next>}");
            }

            htmlWriter.write("\"];\n");
        }

        /**
         * Write this node's previous and next references to htmlWriter as Graphviz Dot edges.
         * @param nodeCache A mapping from LinkedList nodes to DotListNodes used to get node UUIDs.
         */
        public void writeDotEdges(HashMap<Object, DotListNode> nodeCache) throws IllegalAccessException, IOException {
            // Print edge connecting to next (must come first to preserve rankdir)
            Object nextNode = this.getNextNode();
            if (nextNode != null) {
                htmlWriter.write(String.format("  %s%s:next:c -> %s%s:prev:nw;\n",
                        DOT_PREFIX, this.getUUID(), DOT_PREFIX, nodeCache.get(nextNode).getUUID()));
            }

            // Print edge connecting to previous
            Object prevNode = this.getPrevNode();
            if (prevNode != null) {
                htmlWriter.write(String.format("  %s%s:prev:c -> %s%s:next:se;\n",
                        DOT_PREFIX, this.getUUID(), DOT_PREFIX, nodeCache.get(prevNode).getUUID()));
            }
        }
    }
    //endregion

    //region IList public method overrides
    @Override
    public String toString() {
        this.writeOperation("toString()", false);
        return super.toString();
    }

    @Override
    public int size() {
        this.writeOperation("size()", false);
        return super.size();
    }

    @Override
    public boolean equals(Object obj) {
        this.writeOperation(String.format("equals(%s)", obj.toString()), false);
        return super.equals(obj);
    }

    @Override
    public int indexOf(E item) {
        this.writeOperation(String.format("indexOf(%s)", item.toString()), false);
        return super.indexOf(item);
    }

    @Override
    public int indexOf(E item, int pos) {
        this.writeOperation(String.format("indexOf(%s, %d)", item.toString(), pos), false);
        return super.indexOf(item, pos);
    }

    @Override
    public E get(int pos) {
        this.writeOperation(String.format("get(%d)", pos), false);
        return super.get(pos);
    }

    @Override
    public E set(int pos, E item) {
        E retVal = super.set(pos, item);
        this.writeOperation(String.format("set(%d, %s)", pos, item.toString()), true);
        return retVal;
    }

    @Override
    public IList<E> getSubList(int start, int stop) {
        this.writeOperation(String.format("getSubList(%d, %d)", start, stop), false);
        return super.getSubList(start, stop);
    }

    @Override
    public void insert(int pos, E item) {
        super.insert(pos, item);
        this.writeOperation(String.format("insert(%d, %s)", pos, item.toString()), true);
    }

    @Override
    public void add(E item) {
        super.add(item);
        this.writeOperation(String.format("add(%s)", item.toString()), true);
    }

    @Override
    public void addFirst(E item) {
        super.addFirst(item);
        this.writeOperation(String.format("addFirst(%s)", item.toString()), true);
    }

    @Override
    public void addLast(E item) {
        super.addLast(item);
        this.writeOperation(String.format("addLast(%s)", item.toString()), true);
    }

    @Override
    public E remove(int pos) {
        E retVal = super.remove(pos);
        this.writeOperation(String.format("remove(%d)", pos), true);
        return retVal;
    }

    @Override
    public boolean remove(E obj) {
        boolean retVal = super.remove(obj);
        this.writeOperation(String.format("remove(%s)", obj.toString()), true);
        return retVal;
    }

    @Override
    public E removeFirst() {
        E retVal = super.removeFirst();
        this.writeOperation("removeFirst()", true);
        return retVal;
    }

    @Override
    public E removeLast() {
        E retVal = super.removeLast();
        this.writeOperation("removeLast()", true);
        return retVal;
    }

    @Override
    public void removeRange(int start, int stop) {
        super.removeRange(start, stop);
        this.writeOperation(String.format("removeRange(%d, %d)", start, stop), true);
    }

    @Override
    public void makeEmpty() {
        super.makeEmpty();
        this.writeOperation("makeEmpty()", true);
    }

    @Override
    public Iterator<E> iterator() {
        this.writeOperation("iterator()", false);
        return super.iterator();
    }
    //endregion
}