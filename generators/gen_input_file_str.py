import random
import numpy as np
import string


def get_random_string(length):
    letters = string.ascii_lowercase
    result_str = ''.join(random.choice(letters) for i in range(length))
    return result_str


def main():
    file_name = "gen_str_1m_in5.txt"
    with open(file_name, "w") as my_file:
        min_num = 10
        max_num = 100
        cnt = 0
        for i in range(1_000_000):
            num = np.random.randint(min_num, max_num)
            rnd_str = get_random_string(num)

            my_file.write(f"{rnd_str}\n")
            cnt += 1


if __name__ == "__main__":
    main()