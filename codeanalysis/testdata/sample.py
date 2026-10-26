# Sample Python file for testing

def process_data(data):
    # TODO: Add input validation
    # FIXME: Handle edge cases
    print(f"Processing: {data}")

def connect_database():
    # HACK: Temporary connection string
    connection_string = "postgresql://user:password@localhost/db"
    return connection_string

# XXX: This function needs review
def calculate_total(items):
    total = 0
    for item in items:
        total += item
    return total
