Excellent. This is the correct approach. Modifying the existing configuration file is much better than adding a new one.

Based on the decision to use `evolution-parameters.edn` and the discrepancies found in the `failter` usage guide, here is a detailed summary of the files that will change and the specific modifications for each.

### 1. `pcrit.failter.core` (The Failter Adapter)

This file requires the most significant changes to align with the `failter` tool's actual behavior.

*   **`run-contest!` signature change:** The function will be updated to accept a map of parameters that includes the new configuration: `(run-contest! ctx {:keys [... :models :judge-model]})`.
*   **New function `create-model-names-file!`:** A new private helper will be added. It will take the list of model names and write them, one per line, into the `model-names.txt` file inside the `failter-spec` directory.
*   **Corrected `run-shell-command!`:** This helper will be fixed to execute `failter` commands correctly. Instead of running *from within* the `failter-spec` directory, it will run from the parent experiment directory and pass the path to `failter-spec` as a final argument.
*   **Corrected `evaluate` command call:** The call to `failter evaluate` will be modified to conditionally include the `--judge-model <model>` flag if a judge model is provided.
*   **Corrected `report.csv` capture:** The logic that moves the report will be fixed. It will now look for `report.csv` in the root of the `failter-spec` directory (where `failter` creates it) and move it to the final `contest-dir`.

### 2. `pcrit.failter.core_test` (The Failter Adapter's Test)

This test must be updated to reflect the component's new behavior and requirements.

*   **Updated `mock-shell-fn`:** The mock function for `clojure.java.shell/sh` will be modified to simulate the corrected command execution. When it sees the `report` sub-command, it will create the dummy `report.csv` file in the correct location (`failter-spec` root) so the move operation can succeed.
*   **New Assertion:** A new assertion will be added to the main test to verify that the `model-names.txt` file is correctly created inside the `failter-spec` directory and contains the expected content.
*   **Updated `run-contest!` call:** The call to `failter/run-contest!` within the test will be updated to pass in the new `:models` and `:judge-model` parameters.

### 3. `pcrit.command.evaluate` (The `evaluate` Command Logic)

This is the high-level orchestration layer that connects the CLI to the `failter` component.

*   **Load Configuration:** The `evaluate!` function will now read `evolution-parameters.edn` using `config/load-evolution-params`.
*   **Extract Parameters:** It will extract the `:models` and `:judge-model` values from the `:evaluate` section of the loaded configuration.
*   **Validation:** It will add a new validation check to ensure that the `:models` list from the config is not empty.
*   **Updated `failter/run-contest!` call:** It will pass the extracted `:models` and `:judge-model` down to the `failter/run-contest!` function.

### 4. `pcrit.command.evaluate_test` (The `evaluate` Command's Test)

The test for the command logic must be updated to simulate the new configuration dependency.

*   **Updated `setup-test-env`:** The helper function that creates the test environment will be modified to also create a dummy `evolution-parameters.edn` file with an `:evaluate` section. This ensures the command has configuration to read.
*   **Updated Assertions:** The test's assertions will be modified to check that the `failter/run-contest!` mock is called with the correct `:models` and `:judge-model` data that was read from the mock config file.

### 5. `bases/cli/src/pcrit/cli/main.clj` (The CLI Entrypoint)

This file needs a minor update to allow for overriding the configured judge model from the command line for convenience.

*   **New CLI Option:** A new option, `--judge-model`, will be added to the `evaluate` command's option specification in the `command-specs` map.
*   **Updated Handler:** The `do-evaluate` handler will be updated to pass this new option down to the `cmd/evaluate!` function. The `evaluate!` function will then prefer the CLI option over the value in the config file if both are present.
