import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;

// Інтерфейс для функцій
interface Function {
    // Метод для обчислення значення функції для заданого значення x
    double calculate(double x);
    // Метод для обчислення похідної функції
    Function derivative();
    // Метод для отримання рядкового представлення функції з форматуванням чисел
    String toPrettyString(NumberFormat nf);
}

// Клас функцій виду f(x) = const
class Const implements Function {
    public static final Const ZERO = new Const(0);
    public static final Const ONE = new Const(1);
    public static final Const NEGATIVE_ONE = new Const(-1);
    private final double value;

    public Const(double value) {
        this.value = value;
    }

    @Override
    public double calculate(double x) {
        return value;
    }

    @Override
    public Function derivative() {
        // Похідна від константи завжди дорівнює нулю
        return ZERO;
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return nf.format(value);
    }

    public static Const of(double value) {
        return new Const(value);
    }
}

// Клас функцій виду f(x) = kx
class Linear implements Function {
    public static final Linear X = new Linear(1.0) {
        @Override
        public String toPrettyString(NumberFormat nf) {
            return "x";
        }
    };
    private final double coefficient;

    public Linear(double coefficient) {
        this.coefficient = coefficient;
    }

    @Override
    public double calculate(double x) {
        return x * coefficient;
    }

    @Override
    public Function derivative() {
        // Похідна від лінійної функції - це константа, що дорівнює коефіцієнту
        return new Const(coefficient);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("%s*x", nf.format(coefficient));
    }

    public static Linear of(double coefficient) {
        return new Linear(coefficient);
    }
}

// Абстрактний клас для композитних функцій
abstract class Composite implements Function {
    private final ArrayList<Function> terms;

    public ArrayList<Function> terms() {
        return terms;
    }

    public Composite() {
        terms = new ArrayList<>();
    }

    public Composite(Function... terms) {
        this.terms = new ArrayList<>(Arrays.asList(terms));
    }

    public Composite(ArrayList<Function> terms) {
        this.terms = terms;
    }
}

// Клас для функцій суми
class Sum extends Composite {
    public Sum() {
        super();
    }

    public Sum(Function... terms) {
        super(terms);
    }

    public Sum(ArrayList<Function> terms) {
        super(terms);
    }

    @Override
    public double calculate(double x) {
        double result = 0.0;
        for (Function function : terms()) {
            result += function.calculate(x);
        }
        return result;
    }

    @Override
    public Function derivative() {
        ArrayList<Function> derivativeTerms = new ArrayList<>();
        for (Function function : terms()) {
            // Обчислюємо похідну кожного доданку суми
            derivativeTerms.add(function.derivative());
        }
        // Повертаємо суму похідних доданків
        return new Sum(derivativeTerms);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        final StringJoiner joiner = new StringJoiner("+");
        for (Function function : terms()) {
            joiner.add(function.toPrettyString(nf));
        }
        return String.format("(%s)", joiner.toString()).replace("+-", "-");
    }

    public static Sum of(Function... terms) {
        return new Sum(terms);
    }
}

// Клас для функцій множення
class Multiplication extends Composite {
    public Multiplication() {
        super();
    }

    public Multiplication(Function... terms) {
        super(terms);
    }

    public Multiplication(ArrayList<Function> terms) {
        super(terms);
    }

    @Override
    public double calculate(double x) {
        double result = 1.0;
        for (Function function : terms()) {
            result *= function.calculate(x);
        }
        return result;
    }

    @Override
    public Function derivative() {
        ArrayList<Function> derivativeTerms = new ArrayList<>();
        for (int i = 0; i < terms().size(); i++) {
            ArrayList<Function> multipliedTerms = new ArrayList<>(terms());
            Function currentTerm = multipliedTerms.remove(i);
            Function currentTermDerivative = currentTerm.derivative();
            // Перевірка, чи додаємо похідну до множення
            if (!(currentTermDerivative instanceof Const) || ((Const) currentTermDerivative).calculate(0) != 0) {
                multipliedTerms.add(i, currentTermDerivative);
                derivativeTerms.add(new Multiplication(multipliedTerms));
            }
        }
        // Повертаємо суму доданків з похідними
        return new Sum(derivativeTerms);
    }


    @Override
    public String toPrettyString(NumberFormat nf) {
        final StringJoiner joiner = new StringJoiner("*");
        for (Function function : terms()) {
            joiner.add(function.toPrettyString(nf));
        }
        return String.format("(%s)", joiner.toString());
    }

    public static Multiplication of(Function... terms) {
        return new Multiplication(terms);
    }
}

// Клас для функцій степені
class Power extends Composite {
    private final Function base;
    private final double exponent;

    public Power(Function base, double exponent) {
        super(base);
        this.base = base;
        this.exponent = exponent;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.pow(baseValue, exponent);
    }

    @Override
    public Function derivative() {
        return Multiplication.of(
                Const.of(exponent),
                Power.of(base, exponent - 1),
                base.derivative()
        );
    }



    @Override
    public String toPrettyString(NumberFormat nf) {
        String baseString = base.toPrettyString(nf);
        String exponentString = nf.format(exponent);
        return String.format("(%s^%s)", baseString, exponentString);
    }


    public static Power of(Function base, double exponent) {
        return new Power(base, exponent);
    }
}

// Клас для функцій кореня
class Sqrt extends Composite {
    private final Function base;

    public Sqrt(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.sqrt(baseValue);
    }

    @Override
    public Function derivative() {
        // Визначення похідної для функції кореня
        return Multiplication.of(
                Const.of(0.5),
                Power.of(base, -0.5),
                base.derivative()
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("sqrt(%s)", base.toPrettyString(nf));
    }

    public static Sqrt of(Function base) {
        return new Sqrt(base);
    }
}

// Клас для функцій модуля
class Abs extends Composite {
    private final Function base;

    public Abs(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.abs(baseValue);
    }

    @Override
    public Function derivative() {
        Function baseDerivative = base.derivative();
        // Визначення похідної для функції модуля
        return Multiplication.of(
                Multiplication.of(
                        base,
                        Abs.of(Power.of(base, -1))
                ),
                baseDerivative
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("|%s|", base.toPrettyString(nf));
    }

    public static Abs of(Function base) {
        return new Abs(base);
    }
}

// Клас для функцій синуса
class Sin extends Composite {
    private final Function base;

    public Sin(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.sin(baseValue);
    }

    @Override
    public Function derivative() {
        Function baseDerivative = base.derivative();
        // Визначення похідної для функції синуса
        return Multiplication.of(
                Cos.of(base), // Використовуємо Cos.of замість new Cos(base)
                baseDerivative
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("sin(%s)", base.toPrettyString(nf));
    }

    public static Sin of(Function base) {
        return new Sin(base);
    }
}

// Клас для функцій косинуса
class Cos extends Composite {
    public Cos(Function base) {
        super(base);
    }

    @Override
    public double calculate(double x) {
        if (!terms().isEmpty()) {
            double baseValue = terms().get(0).calculate(x);
            return Math.cos(baseValue);
        }
        return 0;
    }

    @Override
    public Function derivative() {
        if (!terms().isEmpty()) {
            Function base = terms().get(0);
            Function baseDerivative = base.derivative();
            // Визначення похідної для функції косинуса
            return Multiplication.of(
                    Const.of(-1),
                    Sin.of(base), // Використовуємо Sin.of замість new Sin(base)
                    baseDerivative
            );
        }
        return Const.ZERO;
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("cos(%s)", terms().isEmpty() ? "" : terms().get(0).toPrettyString(nf));
    }

    public static Cos of(Function base) {
        return new Cos(base);
    }
}

// Клас для функцій тангенсу
class Tan extends Composite {
    private final Function base;

    public Tan(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.tan(baseValue);
    }

    @Override
    public Function derivative() {
        Function baseDerivative = base.derivative();
        // Визначення похідної для функції тангенсу
        return Multiplication.of(
                Power.of(new Cos(base), -2),
                baseDerivative
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("tan(%s)", base.toPrettyString(nf));
    }

    public static Tan of(Function base) {
        return new Tan(base);
    }
}

// Клас для функцій різниці
class Difference extends Composite {
    public Difference() {
        super();
    }

    public Difference(Function... terms) {
        super(terms);
    }

    public Difference(ArrayList<Function> terms) {
        super(terms);
    }

    @Override
    public double calculate(double x) {
        double result = terms().get(0).calculate(x);
        for (int i = 1; i < terms().size(); i++) {
            result -= terms().get(i).calculate(x);
        }
        return result;
    }

    @Override
    public Function derivative() {
        ArrayList<Function> derivativeTerms = new ArrayList<>();
        for (Function function : terms()) {
            // Обчислюємо похідну кожного доданку різниці
            derivativeTerms.add(function.derivative());
        }
        // Повертаємо різницю похідних доданків
        return new Difference(derivativeTerms);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        final StringJoiner joiner = new StringJoiner("-");
        for (Function function : terms()) {
            joiner.add(function.toPrettyString(nf));
        }
        return String.format("(%s)", joiner.toString());
    }

    public static Difference of(Function... terms) {
        return new Difference(terms);
    }
}

public class Main {
    public static void main(String[] args) {
        double x0 = 0.4;

        //  x^2*sqrt(abs(0.7*x-1))-sin(x+0.005)^3
        final Function expression1 =
                Difference.of(
                        Multiplication.of(
                                Power.of(Linear.of(1), 2),
                                Sqrt.of(
                                        Abs.of(
                                                Difference.of(
                                                        Linear.of(0.7),
                                                        Const.of(1)
                                                )
                                        )
                                )
                        ),
                        Power.of(
                                Sin.of(
                                        Sum.of(
                                                Linear.X,
                                                Const.of(0.005)
                                        )
                                ),
                                3
                        )
                );
        final NumberFormat nf = NumberFormat.getInstance();
        System.out.format("\nf1(x) = %s", expression1.toPrettyString(nf)).println();
        System.out.format("f1'(x) = %s", expression1.derivative().toPrettyString(nf)).println();
        System.out.format("f1(0.4) = %f", expression1.calculate(x0)).println();
        System.out.format("f1'(0.4) = %f", expression1.derivative().calculate(x0)).println();

        // Функція f(x) = 0.7/x – tg(0.7*x+0.005)^3
        final Function expression2 =
                Difference.of(
                        Multiplication.of(
                                Const.of(0.7),
                                Power.of(Linear.X, -1)
                        ),
                        Power.of(
                                Tan.of(
                                        Sum.of(
                                                Multiplication.of(
                                                        Const.of(0.7),
                                                        Linear.X
                                                ),
                                                Const.of(0.005)
                                        )
                                ),
                                3
                        )
                );

        System.out.format("\nf2(x) = %s", expression2.toPrettyString(nf)).println();
        System.out.format("f2'(x) = %s", expression2.derivative().toPrettyString(nf)).println();
        System.out.format("f2(0.4) = %f", expression2.calculate(x0)).println();
        System.out.format("f2'(0.4) = %f", expression2.derivative().calculate(x0)).println();
    }
}
