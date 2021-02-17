import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log
import kotlin.math.sin

val ZERO = Value(0.0)
val ONE = Value(1.0)

sealed class Expr {
  open fun simplify() = this
}

data class Value(val value: Double) : Expr() {
  override fun toString(): String {
    return value.toString()
  }
}

data class Variable(val name: String) : Expr() {
  override fun toString(): String {
    return name
  }
}

data class Plus(val x: Expr, val y: Expr) : Expr() {
  override fun simplify(): Expr {
    val x = x.simplify()
    val y = y.simplify()
    return if (x == ZERO && y == ZERO) ZERO
    else if (x == ZERO) y
    else if (y == ZERO) x
    else if (x is Value && y is Value) Value(x.value + y.value)
    else Plus(x, y)
  }

  override fun toString(): String {
    return "($x + $y)"
  }
}

data class Minus(val x: Expr, val y: Expr) : Expr() {
  override fun simplify(): Expr {
    val x = x.simplify()
    val y = y.simplify()
    return if (y.simplify() == ZERO) x.simplify()
    else if (x is Value && y is Value) Value(x.value - y.value)
    else Minus(x, y)
  }

  override fun toString(): String {
    return "($x - $y)"
  }
}

data class Multiply(val x: Expr, val y: Expr) : Expr() {
  override fun simplify(): Expr {
    val x = x.simplify()
    val y = y.simplify()
    return if (x == ZERO || y == ZERO) ZERO
    else if (x == ONE) y
    else if (y == ONE) x
    else if (x is Value && y is Value) Value(x.value * y.value)
    else Multiply(x, y)
  }

  override fun toString(): String {
    return "($x * $y)"
  }
}

data class Divide(val x: Expr, val y: Expr) : Expr() {
  override fun simplify(): Expr {
    val x = x.simplify()
    val y = y.simplify()
    return when {
      x == ZERO -> ZERO
      y == ONE -> x
      x is Value && y is Value -> Value(x.value / y.value)
      else -> Divide(x, y)
    }
  }

  override fun toString(): String {
    return "($x / $y)"
  }
}

data class Sin(val expr: Expr) : Expr() {
  override fun simplify(): Expr {
    val expr = expr.simplify()
    return if (expr is Value) Value(sin(expr.value))
    else Sin(expr)
  }

  override fun toString(): String {
    return "sin($expr)"
  }
}

data class Cos(val expr: Expr) : Expr() {
  override fun simplify(): Expr {
    val expr = expr.simplify()
    return if (expr is Value) Value(cos(expr.value))
    else Cos(expr)
  }

  override fun toString(): String {
    return "cos($expr)"
  }
}

data class Exp(val expr: Expr) : Expr() {
  override fun simplify(): Expr {
    val expr = expr.simplify()
    return if (expr is Value) Value(exp(expr.value))
    else Exp(expr)
  }

  override fun toString(): String {
    return "exp($expr)"
  }
}

data class Log(val expr: Expr) : Expr() {
  override fun simplify(): Expr {
    val expr = expr.simplify()
    return if (expr is Value) Value(log(expr.value, Math.E))
    else Log(expr)
  }

  override fun toString(): String {
    return "log($expr)"
  }
}

fun eval(expr: Expr, env: Map<Variable, Value>): Double {
  return when (expr) {
    is Value -> return expr.value
    is Variable -> return env[expr]?.value ?: throw Error("uninitialized variable ${expr.name}")
    is Sin -> sin(eval(expr.expr, env))
    is Cos -> cos(eval(expr.expr, env))
    is Plus -> eval(expr.x, env) + eval(expr.y, env)
    is Minus -> eval(expr.x, env) - eval(expr.y, env)
    is Multiply -> eval(expr.x, env) * eval(expr.y, env)
    is Divide -> eval(expr.x, env) / eval(expr.y, env)
    is Exp -> exp(eval(expr.expr, env))
    is Log -> log(eval(expr.expr, env), Math.E)
  }
}

fun diff(expr: Expr, variable: Variable): Expr {
  return when (expr) {
    is Value -> ZERO
    is Variable -> if (expr == variable) return ONE else ZERO
    is Sin -> return Cos(expr)
    is Cos -> return Multiply(Value(-1.0), Sin(expr))
    is Plus -> {
      val ll = diff(expr.x, variable)
      val rr = diff(expr.y, variable)
      return Plus(ll, rr).simplify()
    }
    is Minus -> {
      val ll = diff(expr.x, variable)
      val rr = diff(expr.y, variable)
      return Minus(ll, rr).simplify()
    }
    is Multiply -> {
      val x = expr.x.simplify()
      val y = expr.x.simplify()
      if (x is Value) return Multiply(x, diff(y, variable)).simplify()
      if (y is Value) return Multiply(y, diff(x, variable)).simplify()

      val ll = diff(x, variable)
      val rr = diff(y, variable)
      return Plus(Multiply(ll, y), Multiply(rr, x)).simplify()
    }
    is Divide -> {
      val y = expr.y.simplify()
      if (y is Value) return Divide(diff(expr.x, variable), y).simplify()

      val ll = diff(expr.x, variable)
      val rr = diff(y, variable)
      return Divide(Minus(Multiply(ll, y), Multiply(rr, expr.x)), Multiply(y, y)).simplify()
    }
    is Exp -> {
      return Multiply(diff(expr.expr, variable), expr).simplify()
    }
    is Log -> {
      return Divide(ONE, expr.expr).simplify()
    }
  }
}

fun main() {
  val testData = listOf(
    Multiply(Variable("x"), Variable("y")),
    Divide(Variable("x"), Variable("y")),
    Exp(Multiply(Variable("x"), Variable("y"))),
    Log(Multiply(Variable("x"), Divide(Variable("y"), Value(2.0)))),
    Multiply(Multiply(Value(3.0), Variable("y")), Variable("x")),
    Multiply(Multiply(ONE, Value(3.0)), Log(Multiply(Variable("x"), Divide(Variable("y"), Value(2.0))))),
    Divide(Variable("x"), Log(Multiply(Value(3.0), Value(5.0))))
  )
  testData.forEach {
    println("$it'(x) = ${diff(it, Variable("x"))}")
  }
}