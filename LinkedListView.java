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
import java.util.ArrayList;
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

    /** Colour in which to highlight new list components. */
    private static final String NEW_COLOUR = "blue";
    /** Colour in which to highlight modified list components. */
    private static final String MODIFIED_COLOUR = "red";
    /** Character sequence at the end of a node's attribute declaration. */
    private static final String END_NODE_ATTRIBUTES = "];\n";
    /** Whether to highlight inter-operation modifications. */
    private boolean highlightModifications;
    /** Cache of GraphViz Dot list nodes generated on a previous operation. */
    private HashMap<Object, DotListNode> lastDotNodes;
    /** Header node in the previous operation. */
    private Object lastHeadNode;
    /** Tail node in the previous operation. */
    private Object lastTailNode;

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
            + "<style>html, body {\n"
            + "  background-image: url(\"https://www.cs.utexas.edu/~scottm/images/yellow_rock.gif\");\n"
            + "}</style>\n"
            + "</head>\n"
            + "<body>\n"
            + "<script src=\"https://d3js.org/d3.v4.min.js\"></script>\n"
            + "<script src=\"https://unpkg.com/viz.js@1.8.0/viz.js\"></script>\n"
            + "<script src=\"https://unpkg.com/d3-graphviz@0.1.2/build/d3-graphviz.js\"></script>\n"
            + "<h1>CS 314 <code>LinkedList</code> operation transcript</h1>\n"
            + "<h3>Time generated: %s</h3>\n"
            + "<p>Elements highlighted in <span style=\"font-weight: 600; color: blue;\">blue</span> were "
            + "<strong>added</strong> as a result of the last operation.</p>\n"
            + "<p>Elements highlighted in <span style=\"font-weight: 600; color: red;\">red</span> were "
            + "<strong>modified</strong> as a result of the last operation.</p>\n"
            + "<hr/>\n";
    /** Closing statements for HTML graph output files. */
    private static final String HTML_POSTAMBLE =
            "<hr/>\n"
            + "<p>Is something not as you would expect?  Check the "
            + "<a href=\"https://www.cs.utexas.edu/~scottm/cs314/Assignments/A5_LinkedLists.html\">"
            + "<code>LinkedList</code> assignment page</a>!</p>\n"
            + "<p>This report was generated by "
            + "<a href=\"https://github.com/Andrew-William-Smith/CS314-LinkedListView\">"
            + "<code>CS314-LinkedListView</code></a>.</p>\n"
            + "</body>\n</html>";
    /**
     * List operation template.  Must be formatted with:
     * <ul>
     *     <li>Header tag name</li>
     *     <li>Operation name</li>
     *     <li>Timestamp</li>
     *     <li>Caller file name</li>
     *     <li>Caller line number</li>
     *     <li>Header tag name</li>
     * </ul>
     */
    private static final String OPERATION_PREAMBLE =
            "<%s><code>%s</code> at %s from <code>%s:%d</code></%s>\n";
    /** Diagramme template.  Must be formatted with a UUID twice. */
    private static final String DIAGRAMME_PREAMBLE =
            "<div id=\"%s\"></div>\n"
            + "<script>\n"
            + "d3.select('[id=\"%s\"]').graphviz().engine('dot').renderDot(`\n"
            + "strict digraph {\n"
            + "  node[shape=record,penwidth=1.5" + END_NODE_ATTRIBUTES
            + "  edge[penwidth=2" + END_NODE_ATTRIBUTES
            + "  rankdir=LR;\n"
            + "  bgcolor=transparent;\n"
            + "  splines=true;\n"
            + "  ordering=out;\n";
    private static final String DIAGRAMME_POSTAMBLE = "}`);\n</script>\n";
    //endregion

    //region Constructor and field resolution
    /**
     * Construct a new LinkedListView instance, using reflection to determine how the LinkedList is structured.  Updates
     * to the list between operations will be highlighted.
     *
     * @param fileName The name of the file to which to write HTML list diagrams.
     */
    public LinkedListView(String fileName) throws NoSuchFieldException {
        this(fileName, true);
    }

    /**
     * Construct a new LinkedListView instance, using reflection to determine how the LinkedList is structured.
     *
     * @param fileName The name of the file to which to write HTML list diagrams.
     * @param highlightModifications Whether to highlight modifications to the list between operations.
     * @throws NoSuchFieldException If a header node could not be found in LinkedList.
     */
    public LinkedListView(String fileName, boolean highlightModifications) throws NoSuchFieldException {
        this.highlightModifications = highlightModifications;

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

        // Initial diff-checking state
        lastDotNodes = new HashMap<>();
        lastHeadNode = null;
        lastTailNode = null;

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
        StackTraceElement callerStackFrame = Thread.currentThread().getStackTrace()[callerStackOffset];
        if (callerStackFrame.getClassName().equals("LinkedList")) {
            return;
        }

        // Create HTML structure for operation
        String operationId = UUID.randomUUID().toString();
        String headerTag = writeDiagramme ? "h2" : "h4";

        try {
            htmlWriter.write(String.format(OPERATION_PREAMBLE, headerTag, operationName, this.currentTimestamp(),
                    callerStackFrame.getFileName(), callerStackFrame.getLineNumber(), headerTag));
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

    /**
     * Write a GraphViz colour attribute to htmlWriter if an aspect of the graph was modified.
     * @param attributeName The colour attribute to write if modified.
     * @param added Whether this component of the graph was added in this stage.
     * @param modified Whether this component of the graph was not added, but modified in this stage.
     */
    private void writeModifiedColour(String attributeName, boolean added, boolean modified)
            throws IOException {
        String colour = added ? NEW_COLOUR : MODIFIED_COLOUR;
        if (this.highlightModifications && (modified || added)) {
            htmlWriter.write(String.format("%s=%s", attributeName, colour));
        }
    }
    //endregion

    //region List rendering
    /**
     * Write a GraphViz Dot representation of this list to htmlWriter.
     */
    public void writeDiagramme() throws IllegalAccessException, IOException {
        final String headerNodeName = "__HEADER_NAME";
        final String tailNodeName = "__TAIL_NAME";

        // Store DotListNodes in a map: LinkedList node -> DotListNode
        HashMap<Object, DotListNode> dotNodes = new HashMap<>();
        ArrayList<ArrayList<DotListNode>> nodeLevels = new ArrayList<>();
        int[] levelLimits = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};

        // Process from header node
        this.writeExternalVariable(headerNodeName, this.headNodeField.getName());
        Object rawHeadNode = this.headNodeField.get(this);
        DotListNode headNode = this.processNode(rawHeadNode, dotNodes, nodeLevels, 0, levelLimits);
        // Process from tail node (if extant)
        Object rawTailNode = null;
        DotListNode tailNode = null;
        if (this.tailNodeField != null) {
            this.writeExternalVariable(tailNodeName, this.tailNodeField.getName());
            rawTailNode = this.tailNodeField.get(this);
            // In a properly structured list, the tail should have the maximum level
            tailNode = this.processNode(rawTailNode, dotNodes, nodeLevels, levelLimits[1], levelLimits);
        }

        // Print nodes at each level
        this.printRankedNodes(nodeLevels);

        // Print edges
        if (headNode == null) {
            this.writeNullExternalNode(headerNodeName, this.lastHeadNode);
        } else {
            htmlWriter.write(String.format("  %s -> %s%s [", headerNodeName, DotListNode.DOT_PREFIX,
                    headNode.getUUID()));
            writeModifiedColour("color", false, rawHeadNode != this.lastHeadNode);
            htmlWriter.write(END_NODE_ATTRIBUTES);
        }
        if (this.tailNodeField != null) {
            if (tailNode == null) {
                this.writeNullExternalNode(tailNodeName, this.lastTailNode);
            } else {
                htmlWriter.write(String.format("  %s%s -> %s [dir=back,", DotListNode.DOT_PREFIX, tailNode.getUUID(),
                        tailNodeName));
                writeModifiedColour("color", false, rawTailNode != this.lastTailNode);
                htmlWriter.write(END_NODE_ATTRIBUTES);
            }
        }
        htmlWriter.write("  edge[tailclip=false,arrowtail=dot,dir=both" + END_NODE_ATTRIBUTES);

        for (DotListNode node : dotNodes.values()) {
            node.writeDotEdges(dotNodes);
        }

        // Write dummy edges
        if (nodeLevels.size() > 1) {
            htmlWriter.write("  __DUMMY_0");
            for (int i = 1; i < nodeLevels.size(); i++) {
                htmlWriter.write(" -> __DUMMY_" + i);
            }
            htmlWriter.write(" [style=invis" + END_NODE_ATTRIBUTES);
        }

        // Store this run's node cache
        this.lastDotNodes = dotNodes;
        this.lastHeadNode = rawHeadNode;
        this.lastTailNode = rawTailNode;
    }

    /**
     * Print an external variable with the specified name to standard output as a Dot node.
     * @param nodeName The name of the Dot node to print.
     * @param name The name of the Java variable to print.
     */
    private void writeExternalVariable(String nodeName, String name) throws IOException {
        htmlWriter.write("  " + nodeName + "[style=filled,fillcolor=black,fontcolor=white,fontname=monospace,"
                + "shape=ellipse,label=\"" + name + "\"" + END_NODE_ATTRIBUTES);
    }

    /**
     * Draw an edge from the specified external variable's node to a null node.
     * @param nameNode The name of the node containing this variable's name.
     * @param lastTarget The target of this node during the previous operation.
     */
    private void writeNullExternalNode(String nameNode, Object lastTarget) throws IOException {
        htmlWriter.write("  " + nameNode + "_NULL [shape=circle,label=<<B>∅</B>>" + END_NODE_ATTRIBUTES);
        htmlWriter.write("  " + nameNode + " -> " + nameNode + "_NULL [");
        writeModifiedColour("color", false, lastTarget == null);
        htmlWriter.write(END_NODE_ATTRIBUTES);
    }

    /**
     * Recursively populate a map from LinkedList nodes onto DotListNodes, beginning at the specified node.
     * @param startNode The node at which to start populating the map.
     * @param nodeCache The map into which to store node mappings.
     * @param levelNodes A list of nodes in each level of the rendered graph.
     * @param level The level at which this node should be displayed in diagrammes.
     * @param levelLimits int[2] storing the [minimum, maximum] level values thus far.
     * @return The DotListNode corresponding to startNode.
     */
    private DotListNode processNode(Object startNode, HashMap<Object, DotListNode> nodeCache,
            ArrayList<ArrayList<DotListNode>> levelNodes, int level, int[] levelLimits) throws IllegalAccessException {
        // If start node is null, do nothing
        if (startNode == null) {
            return null;
        }

        // If this node has already been mapped, do not map again
        if (nodeCache.containsKey(startNode)) {
            return nodeCache.get(startNode);
        }

        // Otherwise, create a new mapping
        DotListNode mappedNode = new DotListNode(startNode);
        nodeCache.put(startNode, mappedNode);
        // Adjust level bounds and add to level
        if (level < levelLimits[0]) {
            levelLimits[0] = level;
            ArrayList<DotListNode> newLevel = new ArrayList<>();
            newLevel.add(mappedNode);
            levelNodes.add(0, newLevel);
        } else if (level > levelLimits[1]) {
            levelLimits[1] = level;
            ArrayList<DotListNode> newLevel = new ArrayList<>();
            newLevel.add(mappedNode);
            levelNodes.add(newLevel);
        } else {
            levelNodes.get(level - levelLimits[0]).add(mappedNode);
        }

        this.processNode(mappedNode.getNextNode(), nodeCache, levelNodes, level + 1, levelLimits);
        this.processNode(mappedNode.getPrevNode(), nodeCache, levelNodes, level - 1, levelLimits);
        return mappedNode;
    }

    /**
     * Print the nodes in this list, grouped by their levels.
     * @param levelNodes The nodes in the list within each level of the list.
     */
    private void printRankedNodes(ArrayList<ArrayList<DotListNode>> levelNodes) throws IOException {
        for (int level = 0; level < levelNodes.size(); level++) {
            // Write level header
            htmlWriter.write("  {rank=same; __DUMMY_" + level + "[shape=none,label=\"\",height=0,width=0]; ");
            for (DotListNode node : levelNodes.get(level)) {
                node.writeDot();
            }

            htmlWriter.write("}\n");
        }
    }

    /**
     * GraphViz Dot-formatted deep copy of a single LinkedList node.  Instances of this class should not be persisted
     * longer than a single method due to potential changes in the structure of the LinkedList itself.
     */
    private class DotListNode {
        /** Prefix to prepend to Dot node names. */
        public static final String DOT_PREFIX = "__NODE_";

        /** The LinkedList node on which this node is based. */
        private Object baseNode;
        /** A reference to the previous node in the LinkedList. */
        private Object prevNode;
        /** The data stored in (referenced by) this object. */
        private E data;
        /** A reference to the following node in the LinkedList. */
        private Object nextNode;
        /** A unique identifier for this node, used in Dot graph generation because Java hash codes can collide. */
        private UUID uuid;

        /**
         * Construct a single DotListNode from the specified field in the specified object, which should be a LinkedList
         * node.
         *
         * @param baseNode The LinkedList node on which to base this node.
         */
        @SuppressWarnings("unchecked")
        public DotListNode(Object baseNode) throws IllegalAccessException {
            this.uuid = UUID.randomUUID();
            this.baseNode = baseNode;
            // Deep copy from base node
            this.prevNode = nodePrevField.get(baseNode);
            this.data = (E) nodeDataField.get(baseNode);
            this.nextNode = nodeNextField.get(baseNode);
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
         * @return The previous object in this LinkedList.
         */
        public Object getPrevNode() {
            return this.prevNode;
        }

        /**
         * @return The following object in this LinkedList.
         */
        public Object getNextNode() {
            return this.nextNode;
        }

        /**
         * Write this node to htmlWriter as a GraphViz Dot node.
         */
        public void writeDot() throws IOException {
            // Write UUID by integer value, as Dot does not allow hyphens in node names
            htmlWriter.write("  " + DOT_PREFIX + this.getUUID() + "[label=\"");

            if (this.prevNode == null && this.data == null && this.nextNode == null) {
                // Null node: print a simple representation of null
                htmlWriter.write("null");
            } else {
                // Node with value: print a record node
                String dataStr = (this.data == null) ? " null" : " " + this.data.toString().replaceAll("\"", "\\\"");
                htmlWriter.write("{<prev>|<data>" + dataStr + "|<next>}");
            }

            htmlWriter.write("\",");
            // Highlight newly added nodes
            boolean newNode = !lastDotNodes.containsKey(this.baseNode);
            E lastData = null;
            writeModifiedColour("color", newNode, false);
            if (newNode) {
                htmlWriter.write(",");
            } else {
                lastData = lastDotNodes.get(this.baseNode).data;
            }

            // Highlight data according to modification type
            boolean modified = (lastData == null && data != null)
                    || (lastData != null && data == null)
                    || (lastData != null && !lastData.equals(this.data));
            writeModifiedColour("fontcolor", newNode, !newNode && modified);
            htmlWriter.write("];");
        }

        /**
         * Write this node's previous and next references to htmlWriter as Graphviz Dot edges.
         * @param nodeCache A mapping from LinkedList nodes to DotListNodes used to get node UUIDs.
         */
        public void writeDotEdges(HashMap<Object, DotListNode> nodeCache) throws IOException, IllegalAccessException {
            // Do not highlight edges for new nodes
            boolean newNode = !lastDotNodes.containsKey(this.baseNode);

            // Print edge connecting to next (must come first to preserve rankdir)
            if (this.nextNode != null) {
                htmlWriter.write(String.format("  %s%s:next:c -> %s%s:nw [",
                        DOT_PREFIX, this.getUUID(), DOT_PREFIX, nodeCache.get(this.nextNode).getUUID()));
                writeModifiedColour("color", newNode, !newNode
                        && (lastDotNodes.get(this.baseNode).nextNode != this.nextNode));
                // Unconstrain references to header
                if (edgeConnectsHeader(this.baseNode, this.nextNode)) {
                    htmlWriter.write(" constraint=false");
                }
                htmlWriter.write(END_NODE_ATTRIBUTES);
            }

            // Print edge connecting to previous
            if (this.prevNode != null) {
                htmlWriter.write(String.format("  %s%s:prev:c -> %s%s:se [",
                        DOT_PREFIX, this.getUUID(), DOT_PREFIX, nodeCache.get(this.prevNode).getUUID()));
                writeModifiedColour("color", newNode, !newNode
                        && (lastDotNodes.get(this.baseNode).prevNode != this.prevNode));
                // Unconstrain references to header
                if (edgeConnectsHeader(this.baseNode, this.prevNode)) {
                    htmlWriter.write(" constraint=false");
                }
                htmlWriter.write(END_NODE_ATTRIBUTES);
            }
        }

        /**
         * Determine whether the either of the specified nodes in an edge is the
         * list's header node.  Used to improve rendering of circular linked
         * lists.
         *
         * @param node1 The first node in the edge.
         * @param node2 The second node in the edge.
         * @return true if node1 or node2 is the header node, false otherwise.
         */
        private boolean edgeConnectsHeader(Object node1, Object node2) throws IllegalAccessException {
            Object headerNode = headNodeField.get(LinkedListView.this);
            return (node1 == headerNode) || (node2 == headerNode);
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
