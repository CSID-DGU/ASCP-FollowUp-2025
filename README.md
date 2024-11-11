# ASCP
Airline Scheduling Crew Pairing

## Environmental Setting

### OptaPlanner Execution Guide
1. Navigate to the `ASCP/PairingCreater` directory.
2. Run `./gradlew build` to generate the `crew-pairing.jar` file in `build/libs/`.
3. Move `crew-pairing.jar` to the `ASCP/PairingCreater/` directory.
4. Use the following command to execute OptaPlanner:

```
java -jar crew-pairing.jar data/ crewpairing/ {datasize} input_{datasize}.xlsx
```

- If an initial set (`output.xlsx`) exists, use:

```
java -jar crew-pairing.jar data/ crewpairing/ {datasize} input_{datasize}.xlsx output.xlsx
```

5. Ensure that the `output` folder is added to `data/crewpairing/`.

### Reinforcement Learning Execution Guide
1. Add an `input` file to the `dataset` folder within the project root directory.
2. Run `REINFORCE.py` to train the model.
3. More details on [ReinforcementLearning/README.md](https://github.com/CSID-DGU/ASCP/blob/turkish/ReinforcementLearning/README.md)

## Python Packages for Reinforcement Learning

### Using requirements.txt
1. Enter your local or virtual environment (Python 3.8 or higher).
2. Run the following command:

```
pip install -r requirements.txt
```

### PyTorch - CUDA 11.8
Refer to the [Download page](https://pytorch.kr/get-started/locally/) for PyTorch installation.

## License

This project follows the licensing guidelines applicable to OptaPlanner and any relevant dependencies.



## GitHub Role
The following conventions should be adhered to when working on this repository.

### Commit Convention
- `feat`: Add a new feature
- `fix`: Fix a bug
- `docs`: Modify documentation
- `style`: Code formatting, missing semi-colons, etc. (no code changes)
- `refactor`: Refactor code
- `test`: Add or refactor tests; no production code changes
- `chore`: Modify build tasks, package manager configurations, etc.

### PR Convention

| Icon   | Code                       | Description               |
|--------|----------------------------|---------------------------|
| 🎨     | :art                       | Improve structure/format of code |
| ⚡     | :zap                       | Improve performance          |
| 🔥     | :fire                      | Remove code or files         |
| 🐛     | :bug                       | Fix a bug                    |
| 🚑     | :ambulance                 | Critical hotfix              |
| ✨     | :sparkles                  | Introduce new features       |
| 💄     | :lipstick                  | Add/update UI or style files |
| ⏪     | :rewind                    | Revert changes               |
| 🔀     | :twisted_rightwards_arrows | Merge branches               |
| 💡     | :bulb                      | Add/update comments          |
| 🗃     | :card_file_box             | Modify database related files |


