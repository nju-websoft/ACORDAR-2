#!/usr/bin/env python
# -*- coding: UTF-8 -*-
# ================================================================
#   Copyright (C) 2021 * Ltd. All rights reserved.
#   Project ：202111
#   File name   : download_demo.py
#   Author      : yoyo
#   Contact     : cs_jxau@163.com
#   Created date: 2021-11-23 17:55:14
#   Editor      : yoyo
#   Modify Time : 2021-11-23 17:55:14
#   Version     : 1.0
#   IDE         : PyCharm2021
#   License     : Copyright (C) 2019-2022, yichaokeji
#   Description : [Python通过paramiko从远处服务器下载文件资源到本地](https://blog.csdn.net/bang152101/article/details/88861760/)
# ================================================================


"""
通过paramiko从远处服务器下载文件资源到本地
author: gxcuizy
time: 2018-08-01
"""

import paramiko
import os,re,itertools
from stat import S_ISDIR as isdir


def down_from_remote(sftp_obj, remote_dir_name, local_dir_name):
    """远程下载文件"""
    remote_file = sftp_obj.stat(remote_dir_name)
    if isdir(remote_file.st_mode):
        # 文件夹，不能直接下载，需要继续循环
        check_local_dir(local_dir_name)
        print('开始下载文件夹：' + remote_dir_name)
        for remote_file_name in sftp.listdir(remote_dir_name):
            sub_remote = os.path.join(remote_dir_name, remote_file_name)
            sub_remote = sub_remote.replace('\\', '/')
            sub_local = os.path.join(local_dir_name, remote_file_name)
            sub_local = sub_local.replace('\\', '/')
            down_from_remote(sftp_obj, sub_remote, sub_local)
    else:
        # 文件，直接下载
        print('开始下载文件：' + remote_dir_name)
        sftp.get(remote_dir_name, local_dir_name)


def check_local_dir(local_dir_name):
    """本地文件夹是否存在，不存在则创建"""
    if not os.path.exists(local_dir_name):
        os.makedirs(local_dir_name)



if __name__ == "__main__":
    """程序主入口"""
    # 服务器连接信息
    host_name = '112.74.109.3'
    user_name = 'root'
    password = 'Yc888888'
    port = 22
    candidate_part = ["all", "meta", "data"]
    candidate_fold = ["0", "1", "2", "3", "4"]
    pattern = re.compile(r'[0-59]\.[0-59]')
    for part, fold in itertools.product(candidate_part, candidate_fold):
        rank_Path_dir = f'''/home/ttlin/ColBERT/experiments/{part}/{fold}/{lr_batch}/retrieve.py/'''  # 2022-04-30_10.59.47/ranking.tsv
        listdir = os.listdir(rank_Path_dir)
        assert len(listdir) == 1
        print(listdir)
        if pattern.match(listdir[0]):
            rank_Path = listdir[0] + '2022-04-30_10.59.47'
        else:
            rank_Path = "null"

        if part == "all":
            tmp = "ColBERT"
        else:
            tmp = "ColBERT " + part[0]
    # 远程文件路径（需要绝对路径）
    remote_dir = '/www/wwwroot/xxx.com/uploadPath/quantization_package/start-client.zip'
    # 本地文件存放路径（绝对路径或者相对路径都可以）
    local_dir = './start-client.zip'

    # 连接远程服务器
    t = paramiko.Transport((host_name, port))
    t.connect(username=user_name, password=password)
    sftp = paramiko.SFTPClient.from_transport(t)

    # 远程文件开始下载
    down_from_remote(sftp, remote_dir, local_dir)

    # 关闭连接
    t.close()
