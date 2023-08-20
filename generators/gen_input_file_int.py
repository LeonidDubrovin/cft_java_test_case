import random
import numpy as np

def main():
    file_name = "../gen_in2.txt"
    with open(file_name, "w") as myfile:
        min_num = -10**7
        max_num = 10**7
        cnt = 0
        arr = np.random.random_integers(min_num, max_num, 50_000_000)
        for num in arr:
            # num = random.randint(min_num, max_num)
            myfile.write(f"{num}\n")
            cnt += 1

if __name__ == "__main__":
    main()