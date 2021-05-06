import socket
import os
import sys
import socketserver

# define a socket

# sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
'''
ip_port = ('127.0.0.1', 8888)

sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sk.bind(ip_port)
sk.listen(3)
'''


# def handleget():
class MyServer(socketserver.BaseRequestHandler):
    def handle(self):
        conn = self.request
        while True:
            print('server waiting...')
            client_data = conn.recv(1024)
            receive_msg = client_data.decode()
            print(receive_msg)
            HTTP_token = receive_msg.split()
            if HTTP_token[0] == 'GET' or 'POST':
                message = 'HTTP/1.1 200 OK\n' \
                          'Server: Platix\'s Server\n' \
                          'Content-length: '
                # '<html><head>\n' \
                # '<title>PlatixZhangWeb</title>\n' \
                # '</head><body>\n' \
                # '<h1 align="center">PZServer</h1>\n' \
                # '</body></html>'
                # print(HTTP_token[0])
                if HTTP_token[1] != '/':  # not just root
                    file_name = HTTP_token[1]
                    # base_path = str(sys.argv[0]).replace('mainsocket1Linux.py', '')
                    base_path = os.getcwd()
                    # print(base_path)
                    file_dir = base_path + file_name
                    # print(file_dir)
                    if os.path.exists(file_dir):
                        # print("filedir:"+file_dir)
                        # print("Requested File Exists!")
                        f = open(file_dir, 'r', encoding='ISO-8859-1')
                        filesize = os.path.getsize(file_dir)
                        fileline = f.read()
                        if HTTP_token[0] == 'POST':
                            fileline = fileline + "<h3>" + receive_msg + "</h3>"
                            filesize = filesize + 7 + len(receive_msg)
                        # print(fileline)
                        message = message + str(filesize - 7) + '\nContent-type: text/html\n\n' + fileline + '\n\n'
                        f.close()
                        # print(message)
                        conn.sendall(str(message).encode())
                        # break
                    else:
                        file_name = '../../CloudComputingLabs-main/Lab2/src/LinuxVersion/NotFound.html'
                        file_dir = base_path + file_name
                        print("filedir:" + file_dir)
                        f = open(file_dir, 'r', encoding='ISO-8859-1')
                        filesize = os.path.getsize(file_dir)
                        fileline = f.read()
                        # print(fileline)
                        message = message + str(filesize - 7) + '\nContent-type: text/html\n\n' + fileline + '\n\n'
                        f.close()
                        # print(message)
                        conn.sendall(str(message).encode())
                        # print("not found!")
            else:
                message = 'HTTP/1.1 501 Not Implemented\n' \
                          'Server: Platix\'s Server\n' \
                          'Content-length: 104\n' \
                          'Content-type: text/html\n' \
                          '\n' \
                          '<html><head>\n' \
                          '<title>PlatixZhangWeb</title>\n' \
                          '</head><body>\n' \
                          '<h1 align="center">NotImpmted</h1>\n' \
                          '</body></html>'
                conn.sendall(str(message).encode())
            # print(message)
            # conn.sendall(str(message).encode())
            print("Here!")
            conn.close()


if __name__ == '__main__':
    server = socketserver.ThreadingTCPServer(('127.0.0.1', 8888), MyServer)
    server.serve_forever()

