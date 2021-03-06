package simpledb.operator;

import simpledb.exception.DbException;
import simpledb.exception.TransactionAbortedException;
import simpledb.field.Field;
import simpledb.tuple.Tuple;
import simpledb.tuple.TupleDesc;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {

    private static final long serialVersionUID = 1L;

    private JoinPredicate joinPredicate;
    private OpIterator[] children;
    private TupleDesc tupleDesc;
    private Tuple lastMatchOperand;

    /**
     * Constructor. Accepts two children to join and the predicate to join them
     * on
     *
     * @param joinPredicate The predicate to use to join the children
     * @param child1        Iterator for the left(outer) relation to join
     * @param child2        Iterator for the right(inner) relation to join
     */
    public Join(JoinPredicate joinPredicate, OpIterator child1, OpIterator child2) {
        this.joinPredicate = joinPredicate;
        this.children = new OpIterator[2];
        this.children[0] = child1;
        this.children[1] = child2;
        this.tupleDesc = TupleDesc.merge(children[0].getTupleDesc(), children[1].getTupleDesc());
    }

    public JoinPredicate getJoinPredicate() {
        return joinPredicate;
    }

    /**
     * @return the field name of join field1. Should be quantified by
     * alias or table name.
     */
    public String getJoinField1Name() {
        return children[0].getTupleDesc().getFieldName(joinPredicate.getField1());
    }

    /**
     * @return the field name of join field2. Should be quantified by
     * alias or table name.
     */
    public String getJoinField2Name() {
        return children[1].getTupleDesc().getFieldName(joinPredicate.getField2());
    }

    /**
     * @see TupleDesc#merge(TupleDesc, TupleDesc) for possible
     * implementation logic.
     */
    @Override
    public TupleDesc getTupleDesc() {
        return tupleDesc;
    }

    @Override
    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        super.open();
        children[0].open();
        children[1].open();
    }

    @Override
    public void close() {
        super.close();
        children[0].close();
        children[1].close();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        this.close();
        this.open();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     *
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    @Override
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        OpIterator leftOperator = children[0];
        OpIterator rightOperator = children[1];

        for (; lastMatchOperand != null || leftOperator.hasNext(); ) {
            Tuple tupleLeft;
            if (lastMatchOperand != null) {
                tupleLeft = lastMatchOperand;
            } else {
                tupleLeft = leftOperator.next();
            }

            for (; rightOperator.hasNext(); ) {
                Tuple tupleRight = rightOperator.next();

                if (joinPredicate.filter(tupleLeft, tupleRight)) {
                    lastMatchOperand = tupleLeft;
                    return buildJoinTuple(tupleLeft, tupleRight);
                }
            }
            lastMatchOperand = null;
            rightOperator.rewind();
        }

        return null;
    }

    private Tuple buildJoinTuple(Tuple tupleLeft, Tuple tupleRight) {
        Tuple tuple = new Tuple(this.getTupleDesc());
        int i = 0;
        Iterator<Field> fields = tupleLeft.fields();
        while (fields.hasNext()) {
            tuple.setField(i++, fields.next());
        }

        fields = tupleRight.fields();
        while (fields.hasNext()) {
            tuple.setField(i++, fields.next());
        }
        return tuple;
    }

    @Override
    public OpIterator[] getChildren() {
        return children;
    }

    @Override
    public void setChildren(OpIterator[] children) {
        this.children = children;
    }

}
