import logging
import json
import os
import logging.handlers

def get_logger(name):
    if not os.path.exists('logs'):
        os.mkdir('logs')
    log_handler = logging.handlers.TimedRotatingFileHandler('logs/sender.log', 'midnight', encoding='utf8')
    log_format = logging.Formatter('%(asctime)s - %(levelname)s - %(filename)s - %(funcName)s - %(lineno)s - %('
                                   'message)s')
    # log_format = logging.Formatter('%(asctime)s - %(levelname)s - %(name)s- %(message)s')
    log_handler.setFormatter(log_format)
    logger = logging.getLogger(name)
    logger.addHandler(log_handler)
    logger.setLevel(logging.DEBUG)
  
    return logger


def get_logger_handler(name):
    log_handler = logging.handlers.TimedRotatingFileHandler('logs/sender.log', 'midnight', encoding='utf8')
    log_format = logging.Formatter('%(asctime)s - %(levelname)s - %(filename)s - %(funcName)s - %(lineno)s - %('
                                   'message)s')
    log_handler.setFormatter(log_format)

    return log_handler


def prettify(obj):
    if not isinstance(obj, dict) and not isinstance(obj, list):
        return obj.__str__()
    else:
        return json.dumps(obj, sort_keys=True, indent=4)