import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MemoryAllocationLab {

    private static class MemoryBlock {
        int start;
        int size;
        String processName;

        MemoryBlock(int start, int size, String processName) {
            this.start = start;
            this.size = size;
            this.processName = processName;
        }

        boolean isFree() {
            return processName == null;
        }
    }

    private static ArrayList<MemoryBlock> memory = new ArrayList<>();
    private static int totalMemory = 0;
    private static int successfulAllocations = 0;
    private static int failedAllocations = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Memory Allocation Simulator (First-Fit)");
        System.out.println("========================================");
        System.out.println();

        if (args.length != 1) {
            System.err.println("Usage: java MemoryAllocationLab <input_file>");
            return;
        }

        processRequests(args[0]);

        System.out.println();
        System.out.println("========================================");
        System.out.println("Final Memory State");
        System.out.println("========================================");
        printMemoryState();

        System.out.println();
        System.out.println("========================================");
        System.out.println("Memory Statistics");
        System.out.println("========================================");
        displayStatistics();
    }

    private static void processRequests(String filename) {
        System.out.println("Reading from: " + filename);

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String firstLine = br.readLine();
            if (firstLine == null) {
                System.err.println("Error: input file is empty.");
                return;
            }

            totalMemory = Integer.parseInt(firstLine.trim());
            System.out.println("Total Memory: " + totalMemory + " KB");
            System.out.println("----------------------------------------");
            System.out.println();
            System.out.println("Processing requests...");
            System.out.println();

            memory = new ArrayList<>();
            memory.add(new MemoryBlock(0, totalMemory, null));

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length == 0) {
                    continue;
                }

                String command = parts[0];

                if (command.equals("REQUEST")) {
                    if (parts.length != 3) {
                        System.out.println("Invalid REQUEST line: " + line);
                        continue;
                    }
                    String processName = parts[1];
                    int size = Integer.parseInt(parts[2]);
                    allocate(processName, size);

                } else if (command.equals("RELEASE")) {
                    if (parts.length != 2) {
                        System.out.println("Invalid RELEASE line: " + line);
                        continue;
                    }
                    String processName = parts[1];
                    deallocate(processName);

                } else {
                    System.out.println("Unknown command: " + line);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void allocate(String processName, int size) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);

            if (block.isFree() && block.size >= size) {
                int originalSize = block.size;

                block.processName = processName;
                block.size = size;

                int remainingSize = originalSize - size;
                if (remainingSize > 0) {
                    int newStart = block.start + size;
                    MemoryBlock freeBlock = new MemoryBlock(newStart, remainingSize, null);
                    memory.add(i + 1, freeBlock);
                }

                successfulAllocations++;
                System.out.println("REQUEST " + processName + " " + size + " KB -> SUCCESS");
                return;
            }
        }

        failedAllocations++;
        System.out.println("REQUEST " + processName + " " + size + " KB -> FAILED (insufficient memory)");
    }

    private static void deallocate(String processName) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);

            if (!block.isFree() && processName.equals(block.processName)) {
                block.processName = null;
                System.out.println("RELEASE " + processName + " -> SUCCESS");
                return;
            }
        }

        System.out.println("RELEASE " + processName + " -> FAILED (process not found)");
    }

    private static void mergeAdjacentBlocks() {
        for (int i = 0; i < memory.size() - 1; i++) {
            MemoryBlock current = memory.get(i);
            MemoryBlock next = memory.get(i + 1);

            if (current.isFree() && next.isFree()) {
                current.size += next.size;
                memory.remove(i + 1);
                i--;
            }
        }
    }

    private static void printMemoryState() {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);
            int index = i + 1;
            int start = block.start;
            int end = block.start + block.size - 1;

            if (block.isFree()) {
                System.out.printf("Block %d: [%d-%d]  FREE (%d KB)%n",
                        index, start, end, block.size);
            } else {
                System.out.printf("Block %d: [%d-%d]  %s (%d KB) - ALLOCATED%n",
                        index, start, end, block.processName, block.size);
            }
        }
    }

    private static void displayStatistics() {
        int allocatedMemory = 0;
        int freeMemory = 0;
        int numberOfProcesses = 0;
        int numberOfFreeBlocks = 0;
        int largestFreeBlock = 0;

        for (MemoryBlock block : memory) {
            if (block.isFree()) {
                freeMemory += block.size;
                numberOfFreeBlocks++;
                if (block.size > largestFreeBlock) {
                    largestFreeBlock = block.size;
                }
            } else {
                allocatedMemory += block.size;
                numberOfProcesses++;
            }
        }

        double allocatedPercent = 0.0;
        double freePercent = 0.0;
        if (totalMemory > 0) {
            allocatedPercent = (allocatedMemory * 100.0) / totalMemory;
            freePercent = (freeMemory * 100.0) / totalMemory;
        }

        double externalFragmentation = 0.0;
        if (totalMemory > 0 && freeMemory > 0) {
            int fragmentedFree = freeMemory - largestFreeBlock;
            externalFragmentation = (fragmentedFree * 100.0) / totalMemory;
        }

        System.out.printf("Total Memory:           %d KB%n", totalMemory);
        System.out.printf("Allocated Memory:       %d KB (%.2f%%)%n", allocatedMemory, allocatedPercent);
        System.out.printf("Free Memory:            %d KB (%.2f%%)%n", freeMemory, freePercent);
        System.out.printf("Number of Processes:    %d%n", numberOfProcesses);
        System.out.printf("Number of Free Blocks:  %d%n", numberOfFreeBlocks);
        System.out.printf("Largest Free Block:     %d KB%n", largestFreeBlock);
        System.out.printf("External Fragmentation: %.2f%%%n", externalFragmentation);
        System.out.println();
        System.out.printf("Successful Allocations: %d%n", successfulAllocations);
        System.out.printf("Failed Allocations:     %d%n", failedAllocations);
        System.out.println("========================================");
    }
}
