import kotlin.math.cos
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
    if (y.simplify() == ZERO) return x.simplify()
    return Minus(x, y)
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
      else -> Divide(x, y)
    }
  }

  override fun toString(): String {
    return "($x / $y)"
  }
}

data class Sin(val expr: Expr) : Expr() {
  override fun simplify(): Expr {
    return expr.simplify()
  }

  override fun toString(): String {
    return "sin($expr)"
  }
}

data class Cos(val expr: Expr) : Expr() {
  override fun simplify(): Expr {
    return expr.simplify()
  }

  override fun toString(): String {
    return "cos($expr)"
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
      if (expr.x is Value) return Multiply(expr.x, diff(expr.y, variable)).simplify()
      if (expr.y is Value) return Multiply(expr.y, diff(expr.x, variable)).simplify()

      val ll = diff(expr.x, variable)
      val rr = diff(expr.y, variable)
      return Plus(Multiply(ll, expr.y), Multiply(rr, expr.x)).simplify()
    }
    is Divide -> {
      val ll = diff(expr.x, variable)
      val rr = diff(expr.y, variable)
      return Divide(Minus(Multiply(ll, expr.y), Multiply(rr, expr.x)), Multiply(expr.y, expr.y)).simplify()
    }
  }
}

fun main(args: Array<String>) {
  println(diff(Multiply(Variable("x"), Variable("y")), Variable("x")))
  println(diff(Divide(Variable("x"), Variable("y")), Variable("x")))
}