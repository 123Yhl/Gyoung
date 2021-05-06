import socket
import os
import sys
import socketserver

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
                if HTTP_token[1] != '/':  # not root
                    file_name = HTTP_token[1]
                    base_path = os.getcwd()
                    file_dir = base_path + file_name
                    if os.path.exists(file_dir):
                        f = open(file_dir, 'r', encoding='ISO-8859-1')
                        filesize = os.path.getsize(file_dir)
                        fileline = f.read()
                        if HTTP_token[0] == 'POST':
                            fileline = fileline + "<h3>" + receive_msg + "</h3>"
                            filesize = filesize + 7 + len(receive_msg)
                        message = message + str(filesize - 7) + '\nContent-type: text/html\n\n' + fileline + '\n\n'
                        f.close()
                        conn.sendall(str(message).encode())
                    else:
                        file_name = '../../CloudComputingLabs-main/Lab2/src/LinuxVersion/NotFound.html'
                        file_dir = base_path + file_name
                        print("filedir:" + file_dir)
                        f = open(file_dir, 'r', encoding='ISO-8859-1')
                        filesize = os.path.getsize(file_dir)
                        fileline = f.read()
                        message = message + str(filesize - 7) + '\nContent-type: text/html\n\n' + fileline + '\n\n'
                        f.close()
                        conn.sendall(str(message).encode())
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
            print("Here!")
            conn.close()

            

if __name__ == '__main__':
    server = socketserver.ThreadingTCPServer(('127.0.0.1', 8888), MyServer)
    server.serve_forever()

