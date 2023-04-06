package main.rice.node;

import main.rice.obj.APyObj;
import main.rice.obj.PyListObj;
import java.util.List;

/**
 * A representation of a node for generating PyListObjs.
 *
 * @param <InnerType> the type of object stored within the lists generated by this node
 */
public class PyListNode<InnerType extends APyObj> extends AIterablePyNode<
        PyListObj<InnerType>, InnerType> {

    /**
     * Constructor fot a PyListNode; stores a reference to its child node (used to
     * generate its contained elements).
     *
     * @param child a node representing the valid elements for this list
     */
    public PyListNode(APyNode<InnerType> child) {
        this.leftChild = child;
    }

    /**
     * Helper function for generating a PyListObj.
     *
     * @param innerVals the elements to be contained by the generated PyListObj
     * @return a PyListObj object encapsulating the innerVals
     */
    @Override
    protected PyListObj<InnerType> genObj(List<InnerType> innerVals) {
        return new PyListObj<>(innerVals);
    }
}