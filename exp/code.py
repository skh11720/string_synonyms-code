class ExpContainer:
    def __init__(self, param_title, data_title, value_title):
        self.param_index = {}
        self.data_index = {}
        self.value_index = {}

        self.param_inv_index = {}
        self.data_inv_index = {}
        self.value_inv_index = {}

        self.data_set = []
        self.param_set = []
        self.value_map = {}

        self.param_title = param_title
        self.data_title = data_title
        self.value_title = value_title

        self.debug = False

    def clear_indexes(self):
        self.param_index.clear()
        self.data_index.clear()
        self.value_index.clear()

    def add_result(self, param_content, data_content, value_content):
        if param_content not in self.param_set:
            self.param_set.append(param_content)
        param_index = self.param_set.index(param_content)

        if data_content not in self.data_set:
            self.data_set.append(data_content)
        data_index = self.data_set.index(data_content)

        self.value_map[(data_index, param_index)] = value_content

    def save_as_csv(self, filename):
        if self.debug:
            print(self.data_set)
        with open(filename, "w") as w:
            for value_index in range(len(self.value_title)):
                w.write("\"" + str(self.value_title[value_index]) + "\",")
                for param in self.param_set:
                    w.write("\"")
                    for i in range(len(self.param_title)):
                        title = param_title[i]
                        if title != "cmd_alg" and title != "cmd_alg_v":
                            w.write(title + ":")
                        w.write(str(param[title]) + " ")
                    w.write("\",")
                w.write("\n")

                for data_index in range(len(self.data_set)):
                    w.write("\"" + str(self.data_set[data_index]) + "\",")
                    for param_index in range(len(self.param_set)):
                        try:
                            value = self.value_map[(data_index, param_index)]
                            w.write("\"" + str(value[self.value_title[value_index]]) + "\",")
                        except KeyError:
                            w.write("\"\",")

                    w.write("\n")

    def parse_title(self, string):
        self.clear_indexes()
        if string.startswith("#"):
            temp = string[1:].strip()
        else:
            temp = string.strip()
        is_started = False
        title = ""

        index = 0

        for c in temp:
            if c == "\"":
                if is_started:
                    if title in self.param_title:
                        self.param_index[title] = index
                        self.param_inv_index[index] = title
                    if title in self.data_title:
                        self.data_index[title] = index
                        self.data_inv_index[index] = title
                    if title in self.value_title:
                        self.value_index[title] = index
                        self.value_inv_index[index] = title

                    index += 1
                    title = ""

                is_started = not is_started
            elif is_started:
                title += c
        if self.debug:
            print(self.param_index)
            print(self.data_index)
            print(self.value_index)

    def parse_content(self, string):
        temp = string.strip()
        is_quote_started = False
        is_started = False
        content = ""

        param_content = {}
        data_content = []
        value_content = {}

        index = 0

        for c in temp:
            if c == "\"":
                if is_quote_started:

                    if index in self.param_index.values():
                        param_content[self.param_inv_index[index]] = content
                    if index in self.data_index.values():
                        data_content.append(content)
                    if index in self.value_index.values():
                        value_content[self.value_inv_index[index]] = content

                    index += 1
                    content = ""

                is_quote_started = not is_quote_started
            elif is_quote_started or is_started:
                if is_started and c == ' ':
                    is_started = not is_started

                    if index in self.param_index.values():
                        param_content[self.param_inv_index[index]] = content
                    if index in self.data_index.values():
                        data_content.append(content)
                    if index in self.value_index.values():
                        value_content[self.value_inv_index[index]] = content

                    index += 1
                    content = ""

                else:
                    content += c
            elif c != " ":
                is_started = not is_started
                content = c

        if content != "":

            if index in self.param_index.values():
                param_content[self.param_inv_index[index]] = content
            if index in self.data_index.values():
                data_content.append(content)
            if index in self.value_index.values():
                value_content[self.value_inv_index[index]] = content

        return param_content, data_content, value_content

    def load(self, files):
        for file in files:
            with open(file, 'r') as r:
                for line in r:
                    if line.startswith("#"):
                        self.parse_title(line)
                    else:
                        x_content, y_content, value_content = self.parse_content(line)
                        self.add_result(x_content, y_content, value_content)


if __name__ == "__main__":
    files = ["JoinBK_2.0", "JoinBKSP_2.1", "JoinMH_2.0", "JoinMHSP_2.0"]  # , "JoinMin_2.0", "JoinNaive_2.0"

    param_title = ["cmd_alg", "cmd_alg_v", "cmd_K", "cmd_qSize"]
    data_title = ["cmd_dataOnePath", "cmd_dataTwoPath", "cmd_rulePath", "cmd_oneSideJoin"]
    value_title = ["Final Result Size", "Result_0_Total_Time", "Result_3_1_Index_Building_Time",
                   "Result_3_2_Join_Time", "hm_resizeCount", "hm_removeCount"]

    container = ExpContainer(param_title, data_title, value_title)
    container.load(files)

    container.save_as_csv("result.csv")
