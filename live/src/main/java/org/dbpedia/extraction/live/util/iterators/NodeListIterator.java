package org.dbpedia.extraction.live.util.iterators;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;


/**
 * An iterator for nodes in an xml document. Nodes are iterated based on an
 * xpath expression.
 *
 * @author raven
 */
public class NodeListIterator
        implements Iterator<Node> {
    private NodeList nodeList;
    private int index;

    public NodeListIterator(NodeList nodeList) {
        this.nodeList = nodeList;
    }

    @Override
    public boolean hasNext() {
        return index < nodeList.getLength();
    }

    @Override
    public Node next() {
        if (!hasNext())
            return null;

        return nodeList.item(index++);
    }

    @Override
    public void remove() {
        throw new RuntimeException("Operation not supported");
    }
}
