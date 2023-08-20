def main():
    file_name = "out.txt"
    result = True

    with open(file_name, "r") as myfile:
        last_num = myfile.readline().rstrip('\n')
        last_num = int(float(last_num))

        cnt = 0
        num = True
        while num:
            num = myfile.readline().rstrip('\n')
            if num:
                int_num = int(float(num))


                if int_num < last_num:
                    result = False
                last_num = int_num
                cnt += 1
                # print(cnt)

    print(result)

if __name__ == "__main__":
    main()