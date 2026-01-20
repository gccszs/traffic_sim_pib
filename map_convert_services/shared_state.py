"""
Shared state module for maintaining simulation engine connections and info
Used to share state between web_app.py and simulation_service.py
"""
import asyncio
from collections import defaultdict, deque
from vo.sim_data_vo import SimInfo

# Shared simulation info dictionary
id_infos = defaultdict(SimInfo)

# Queue for messages to be forwarded to Java backend
# Key: user_id, Value: deque of messages
forward_queue = defaultdict(lambda: deque(maxlen=100))  # Limit queue size

def get_simulation_info(user_id: str) -> SimInfo:
    """Get simulation info for a given user ID"""
    return id_infos[user_id]

def set_simulation_info(user_id: str, sim_info: SimInfo):
    """Set simulation info for a given user ID"""
    id_infos[user_id] = sim_info

def get_all_simulation_ids():
    """Get all active simulation IDs"""
    return list(id_infos.keys())

def remove_simulation_info(user_id: str):
    """Remove simulation info for a given user ID"""
    if user_id in id_infos:
        del id_infos[user_id]

def enqueue_forward_message(user_id: str, message: dict):
    """Add a message to the forward queue for a specific user"""
    forward_queue[user_id].append(message)

def get_forward_messages(user_id: str) -> list:
    """Get all forward messages for a specific user"""
    messages = list(forward_queue[user_id])
    forward_queue[user_id].clear()  # Clear after retrieving
    return messages

def has_forward_messages(user_id: str) -> bool:
    """Check if there are forward messages for a specific user"""
    return len(forward_queue[user_id]) > 0