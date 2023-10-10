package org.hisp.dhis.lib.expression.syntax

import org.hisp.dhis.lib.expression.ast.Node
import org.hisp.dhis.lib.expression.ast.Node.Companion.groupOperators
import org.hisp.dhis.lib.expression.ast.NodeType
import org.hisp.dhis.lib.expression.ast.Nodes.*
import org.hisp.dhis.lib.expression.ast.Nodes.Companion.propagateModifiers
import org.hisp.dhis.lib.expression.ast.Position
import org.hisp.dhis.lib.expression.ast.Position.Companion.addWhitespace
import org.hisp.dhis.lib.expression.spi.IllegalExpressionException
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

/**
 * A [ParseContext] that builds a [Node]-tree using [Node.Factory].
 *
 * @author Jan Bernitt
 */
class Parser private constructor(
    fragments: List<Fragment>,
    private val factoriesByType: MutableMap<NodeType, Node.Factory>
) : ParseContext {

    private val fragmentsByName: MutableMap<String, Fragment>
    private val stack = LinkedList<Node<*>>()

    private var root: Node<*>? = null

    init {
        fragmentsByName = mapByName(fragments)
    }

    fun withFragments(vararg fragments: Fragment): Parser {
        fragmentsByName.putAll(mapByName(listOf(*fragments)))
        return this
    }

    fun withFactory(type: NodeType, factory: Node.Factory): Parser {
        factoriesByType[type] = factory
        return this
    }

    override fun fragment(name: String): Fragment? {
        return fragmentsByName[name]
    }

    override fun beginNode(type: NodeType, start: Position?, value: String, create: Node.Factory?) {
        val f = create ?: factoriesByType[type]
        ?: throw UnsupportedOperationException("No factory for type: $type")
        val node: Node<*> = f.create(type, value)
        node.setStart(start)
        if (stack.isEmpty()) {
            val r = ParenthesesNode(NodeType.PAR, "")
            r.addChild(node)
            stack.addLast(r)
            root = r;
        }
        else {
            stack.last.addChild(node)
        }
        stack.addLast(node)
    }

    override fun endNode(type: NodeType, end: Position?) {
        val node = stack.removeLast()
        node.setEnd(end)
    }

    companion object {
        private val DEFAULT_FACTORIES: MutableMap<NodeType, Node.Factory> = EnumMap(
            NodeType::class.java)

        private fun addFactory(type: NodeType, factory: (type: NodeType, rawValue: String) -> Node<*>) {
            DEFAULT_FACTORIES[type] = Node.Factory.new(factory)
        }

        init {
            // complex nodes (have children)
            addFactory(NodeType.PAR, ::ParenthesesNode)
            addFactory(NodeType.ARGUMENT, ::ArgumentNode)
            addFactory(NodeType.FUNCTION, ::FunctionNode)
            addFactory(NodeType.MODIFIER, ::ModifierNode)
            addFactory(NodeType.DATA_ITEM, ::DataItemNode)
            addFactory(NodeType.VARIABLE, ::VariableNode)
            addFactory(NodeType.UNARY_OPERATOR, ::UnaryOperatorNode)
            addFactory(NodeType.BINARY_OPERATOR, ::BinaryOperatorNode)

            // simple nodes
            addFactory(NodeType.STRING, ::Utf8StringNode)
            addFactory(NodeType.NAMED_VALUE, ::TextNode)
            addFactory(NodeType.UID, ::TextNode)
            addFactory(NodeType.IDENTIFIER, ::TextNode)
            addFactory(NodeType.NUMBER, ::NumberNode)
            addFactory(NodeType.INTEGER, ::IntegerNode)
            addFactory(NodeType.DATE, ::DateNode)
            addFactory(NodeType.BOOLEAN, ::BooleanNode)
            addFactory(NodeType.NULL, ::ConstantNode)
        }

        fun withFragments(fragments: List<Fragment>): Parser {
            return Parser(fragments, EnumMap(DEFAULT_FACTORIES))
        }

        fun parse(expr: String, fragments: List<Fragment>, annotate: Boolean): Node<*> {
            val parser = withFragments(fragments)
            val wsTokens = Expr.parse(expr, parser, annotate)
            val root = parser.root ?: throw IllegalExpressionException("Empty expression")
            if (annotate) {
                addWhitespace(root, wsTokens)
            }
            propagateModifiers(root)
            groupOperators(root)
            return root
        }

        private fun mapByName(functions: List<Fragment>): MutableMap<String, Fragment> {
            return functions.stream().collect(Collectors.toUnmodifiableMap(
                Function { obj: Fragment -> obj.name() }, Function.identity()))
        }
    }
}
