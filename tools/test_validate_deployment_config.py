import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('validator', Path(__file__).with_name('validate_deployment_config.py'))
validator = importlib.util.module_from_spec(spec); spec.loader.exec_module(validator)

class DeploymentConfigurationTest(unittest.TestCase):
    def test_duplicate_server_and_secret_values_are_not_logged(self):
        with tempfile.TemporaryDirectory() as directory:
            config = Path(directory) / 'application.yml'
            config.write_text('server:\n  port: 18088\npassword: never-log-this-secret\nserver:\n  address: 127.0.0.1\n', encoding='utf-8')
            errors = io.StringIO()
            with contextlib.redirect_stderr(errors): self.assertFalse(validator.validate(config))
            self.assertNotIn('never-log-this-secret', errors.getvalue())
            self.assertIn('line 4', errors.getvalue())
    def test_nested_configuration_is_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            config = Path(directory) / 'application.yml'; config.write_text('server:\n  port: 18088\nspring:\n  mail:\n    port: 25\n', encoding='utf-8')
            self.assertTrue(validator.validate(config))

if __name__ == '__main__': unittest.main()
