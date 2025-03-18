package cli.li.resolver.thread;

import java.util.concurrent.Callable;

import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.solver.ICaptchaSolver;

/**
 * Task for solving a CAPTCHA
 */
public record CaptchaSolveTask(ICaptchaSolver solver, CaptchaRequest request) implements Callable<String> {

    @Override
    public String call() throws Exception {
        return solver.solve(request);
    }
}