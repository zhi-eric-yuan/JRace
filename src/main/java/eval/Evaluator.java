/*
 * Created by Zhi Yuan
 */
package eval;

import org.apache.commons.math.analysis.MultivariateFunction;

/**
 * @author yuan
 * Created on Dec 2, 2011
 *
 */
public interface Evaluator extends MultivariateFunction {

	double evaluate(int row, int col);

}
