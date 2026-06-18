import re

TRANSITIONS_FILE_PATH = 'logs/transitions_log.txt'
OUTPUT_FILE_PATH = 'logs/regex_output.txt'

source = open(TRANSITIONS_FILE_PATH, 'r')
candidate = source.readlines()[0]
source.close()

output = open(OUTPUT_FILE_PATH, 'w')
output.write(candidate)
output.close()

while True:
    f = open(OUTPUT_FILE_PATH, 'r')
    transitions = f.readlines()[0]
    f.close()

    pattern = r'(T0)(.*?)((T1)(.*?)(T2)(.*?)(T3)|(T4)(.*?)(T5)|(T6)(.*?)(T7)(.*?)(T8))(.*?)(T9)'
    replace = r'\g<2>\g<5>\g<7>\g<10>\g<13>\g<15>\g<17>'

    match = re.subn(pattern, replace, transitions)
    print(match)

    f = open(OUTPUT_FILE_PATH, 'w')
    f.write(match[0])
    f.close()
    
    if match[1] == 0:
        print('=================')
        print('   TEST FAILED   ')
        print('=================')
        break
    if match[0] == '':
        print('=================')
        print('   TEST PASSED   ')
        print('=================')
        break
